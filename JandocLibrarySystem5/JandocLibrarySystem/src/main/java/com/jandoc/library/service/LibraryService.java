package com.jandoc.library.service;

import com.jandoc.library.config.MongoConfig;
import com.jandoc.library.exception.BookUnavailableException;
import com.jandoc.library.exception.DuplicateBorrowException;
import com.jandoc.library.exception.NoActiveBorrowException;
import com.jandoc.library.exception.UserNotFoundException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Core transaction logic: borrowing and returning books.
 * <p>
 * Keeps the {@code books} collection ({@code availableCopies}) and the
 * {@code users} collection (embedded {@code borrowedBooks} array) in sync
 * for every transaction — that embedded array remains the source of truth
 * the UI reads from. In addition, each transaction is mirrored into two
 * standalone top-level collections, {@code borrow} and {@code returned},
 * purely so the transaction history is easy to browse directly in
 * MongoDB Compass without having to drill into a user's embedded array:
 * a borrow writes a document into {@code borrow}; returning that book
 * deletes it from {@code borrow} and writes the completed record into
 * {@code returned}. Business-rule failures (no stock, unknown member,
 * duplicate borrow, etc.) are reported as checked
 * {@link com.jandoc.library.exception.LibraryException} subtypes rather than
 * magic strings, so callers can't forget to handle a failure case and each
 * failure can be formatted or localized independently.
 */
public class LibraryService {

    /** Standard loan period applied to every new borrow. */
    private static final long LOAN_PERIOD_DAYS = 14;
    private static final long MILLIS_PER_DAY = 24L * 60 * 60 * 1000;

    private final MongoCollection<Document> userCollection;
    private final MongoCollection<Document> bookCollection;
    private final MongoCollection<Document> borrowCollection;
    private final MongoCollection<Document> returnedCollection;

    public LibraryService() {
        MongoDatabase db = MongoConfig.getDatabase();
        this.userCollection = db.getCollection("users");
        this.bookCollection = db.getCollection("books");
        this.borrowCollection = db.getCollection("borrow");
        this.returnedCollection = db.getCollection("returned");
    }

    /**
     * Borrows a book on behalf of a member.
     * <p>
     * The stock check-and-decrement is performed as a single atomic
     * {@code findOneAndUpdate} (filtered on {@code availableCopies > 0}),
     * so two borrow requests racing for the last copy can never both
     * succeed and drive the count negative.
     *
     * @param userId the borrowing member's id
     * @param isbn   ISBN of the book to borrow
     * @throws UserNotFoundException    if no member exists with {@code userId}
     * @throws DuplicateBorrowException if the member already has this ISBN checked out
     * @throws BookUnavailableException if no copies of the book are currently available
     */
    public void borrowBook(ObjectId userId, String isbn)
            throws UserNotFoundException, DuplicateBorrowException, BookUnavailableException {

        Document user = userCollection.find(Filters.eq("_id", userId)).first();
        if (user == null) {
            throw new UserNotFoundException(userId.toHexString());
        }

        if (hasActiveBorrow(user, isbn)) {
            throw new DuplicateBorrowException(isbn);
        }

        // Atomically claim a copy: only matches while availableCopies > 0,
        // so concurrent borrows can never push the count below zero.
        Bson stockFilter = Filters.and(Filters.eq("isbn", isbn), Filters.gt("availableCopies", 0));
        Document book = bookCollection.findOneAndUpdate(
                stockFilter,
                Updates.inc("availableCopies", -1),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.BEFORE)
        );
        if (book == null) {
            throw new BookUnavailableException(isbn);
        }

        Document transactionLog = buildTransactionLog(book, isbn);

        Document updatedUser = userCollection.findOneAndUpdate(
                Filters.eq("_id", userId),
                Updates.push("borrowedBooks", transactionLog)
        );

        if (updatedUser == null) {
            // The member vanished between the first lookup and this write;
            // undo the stock claim so inventory stays accurate.
            bookCollection.updateOne(Filters.eq("isbn", isbn), Updates.inc("availableCopies", 1));
            throw new UserNotFoundException(userId.toHexString());
        }

        // Mirror this transaction into the standalone "borrow" collection
        // so it's visible on its own in Compass, separate from the user's
        // embedded array.
        Document borrowRecord = new Document("userId", userId)
                .append("memberName", user.getString("name"))
                .append("bookId", book.getObjectId("_id"))
                .append("title", book.getString("title"))
                .append("isbn", isbn)
                .append("borrowDate", transactionLog.getDate("borrowDate"))
                .append("dueDate", transactionLog.getDate("dueDate"))
                .append("status", "active");
        borrowCollection.insertOne(borrowRecord);
    }

    /**
     * Returns a book on behalf of a member.
     * <p>
     * Marking the embedded borrow record as returned is a single atomic
     * {@code findOneAndUpdate} filtered on {@code borrowedBooks.returned == false},
     * so a double-click (or two requests racing) can't mark the same
     * record returned twice and credit {@code availableCopies} more than once.
     *
     * @param userId the returning member's id
     * @param isbn   ISBN of the book being returned
     * @throws UserNotFoundException   if no member exists with {@code userId}
     * @throws NoActiveBorrowException if the member has no active borrow of this ISBN
     */
    public void returnBook(ObjectId userId, String isbn)
            throws UserNotFoundException, NoActiveBorrowException {

        Bson activeBorrowFilter = Filters.and(
                Filters.eq("_id", userId),
                Filters.eq("borrowedBooks.isbn", isbn),
                Filters.eq("borrowedBooks.returned", false)
        );

        // Default returnDocument is BEFORE, so updatedUser still holds the
        // pre-update borrow entry (borrowDate/dueDate) — reused below when
        // mirroring the completed transaction into "returned".
        Document updatedUser = userCollection.findOneAndUpdate(
                activeBorrowFilter,
                Updates.combine(
                        Updates.set("borrowedBooks.$.returned", true),
                        Updates.set("borrowedBooks.$.returnDate", new Date())
                )
        );

        if (updatedUser == null) {
            if (userCollection.find(Filters.eq("_id", userId)).first() == null) {
                throw new UserNotFoundException(userId.toHexString());
            }
            throw new NoActiveBorrowException(isbn);
        }

        bookCollection.updateOne(Filters.eq("isbn", isbn), Updates.inc("availableCopies", 1));

        // Move the matching record out of the standalone "borrow"
        // collection and into "returned" so Compass reflects the same
        // transaction history the embedded array now shows.
        Document activeEntry = findActiveEntry(updatedUser, isbn);
        Date returnDate = new Date();
        Date dueDate = activeEntry != null ? activeEntry.getDate("dueDate") : null;
        Date borrowDate = activeEntry != null ? activeEntry.getDate("borrowDate") : null;
        boolean wasOverdue = dueDate != null && dueDate.before(returnDate);

        borrowCollection.deleteOne(Filters.and(Filters.eq("userId", userId), Filters.eq("isbn", isbn)));

        Document returnedRecord = new Document("userId", userId)
                .append("memberName", updatedUser.getString("name"))
                .append("bookId", activeEntry != null ? activeEntry.getObjectId("bookId") : null)
                .append("title", activeEntry != null ? activeEntry.getString("title") : null)
                .append("isbn", isbn)
                .append("borrowDate", borrowDate)
                .append("dueDate", dueDate)
                .append("returnDate", returnDate)
                .append("wasOverdue", wasOverdue);
        returnedCollection.insertOne(returnedRecord);
    }

    /**
     * @return the currently active (not yet returned) borrow records for a member,
     *         or an empty list if the member doesn't exist or has none.
     */
    public List<Document> getActiveBorrows(ObjectId userId) {
        List<Document> active = new ArrayList<>();
        Document user = userCollection.find(Filters.eq("_id", userId)).first();
        if (user == null) return active;

        for (Document entry : borrowedBooksOf(user)) {
            if (!entry.getBoolean("returned", false)) {
                active.add(entry);
            }
        }
        return active;
    }

    /**
     * Reads every document in the standalone {@code borrow} collection
     * (books currently checked out, across all members), most recent first.
     *
     * @param searchTerm optional filter matched against memberName, title, or isbn (case-insensitive);
     *                    pass {@code null} or blank to return every active borrow
     */
    public List<Document> getAllBorrowRecords(String searchTerm) {
        return queryTransactionCollection(borrowCollection, searchTerm);
    }

    /**
     * Reads every document in the standalone {@code returned} collection
     * (completed borrow history, across all members), most recent first.
     *
     * @param searchTerm optional filter matched against memberName, title, or isbn (case-insensitive);
     *                    pass {@code null} or blank to return the full return history
     */
    public List<Document> getAllReturnedRecords(String searchTerm) {
        return queryTransactionCollection(returnedCollection, searchTerm);
    }

    private List<Document> queryTransactionCollection(MongoCollection<Document> collection, String searchTerm) {
        List<Document> results = new ArrayList<>();
        FindIterable<Document> iterable;

        if (searchTerm == null || searchTerm.isBlank()) {
            iterable = collection.find();
        } else {
            String regex = ".*" + Pattern.quote(searchTerm) + ".*";
            iterable = collection.find(Filters.or(
                    Filters.regex("memberName", regex, "i"),
                    Filters.regex("title", regex, "i"),
                    Filters.regex("isbn", regex, "i")
            ));
        }
        for (Document d : iterable) {
            results.add(d);
        }
        return results;
    }

    // ---- helpers -----------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<Document> borrowedBooksOf(Document user) {
        List<Document> borrowed = (List<Document>) user.get("borrowedBooks");
        return borrowed != null ? borrowed : new ArrayList<>();
    }

    private boolean hasActiveBorrow(Document user, String isbn) {
        for (Document entry : borrowedBooksOf(user)) {
            if (isbn.equals(entry.getString("isbn")) && !entry.getBoolean("returned", false)) {
                return true;
            }
        }
        return false;
    }

    /** Finds the still-active (unreturned) embedded borrow entry for the given ISBN, if any. */
    private Document findActiveEntry(Document user, String isbn) {
        for (Document entry : borrowedBooksOf(user)) {
            if (isbn.equals(entry.getString("isbn")) && !entry.getBoolean("returned", false)) {
                return entry;
            }
        }
        return null;
    }

    private Document buildTransactionLog(Document book, String isbn) {
        Date borrowDate = new Date();
        Date dueDate = new Date(borrowDate.getTime() + (LOAN_PERIOD_DAYS * MILLIS_PER_DAY));

        return new Document("bookId", book.getObjectId("_id"))
                .append("title", book.getString("title"))
                .append("isbn", isbn)
                .append("borrowDate", borrowDate)
                .append("dueDate", dueDate)
                .append("returned", false);
    }
}
