package com.jandoc.library.service;

import com.jandoc.library.config.MongoConfig;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all CRUD operations for the "books" collection.
 */
public class BookService {

    private final MongoCollection<Document> bookCollection;

    public BookService() {
        MongoDatabase db = MongoConfig.getDatabase();
        this.bookCollection = db.getCollection("books");
    }

    // Create: Add a new book
    public void addBook(String title, String author, String isbn, String category, int totalCopies) {
        Document book = new Document("title", title)
                .append("author", author)
                .append("isbn", isbn)
                .append("category", category)
                .append("totalCopies", totalCopies)
                .append("availableCopies", totalCopies);

        bookCollection.insertOne(book);
    }

    // Read: Find a book by ISBN
    public Document findBookByIsbn(String isbn) {
        return bookCollection.find(Filters.eq("isbn", isbn)).first();
    }

    // Read: Get every book, optionally filtered by a search term (title/author/isbn/category)
    public List<Document> getAllBooks(String searchTerm) {
        List<Document> results = new ArrayList<>();
        FindIterable<Document> iterable;

        if (searchTerm == null || searchTerm.isBlank()) {
            iterable = bookCollection.find();
        } else {
            String regex = ".*" + java.util.regex.Pattern.quote(searchTerm) + ".*";
            iterable = bookCollection.find(Filters.or(
                    Filters.regex("title", regex, "i"),
                    Filters.regex("author", regex, "i"),
                    Filters.regex("isbn", regex, "i"),
                    Filters.regex("category", regex, "i")
            ));
        }
        for (Document d : iterable) {
            results.add(d);
        }
        return results;
    }

    // Update: Modify a book's core details
    public void updateBook(ObjectId id, String title, String author, String category, int totalCopies, int availableCopies) {
        bookCollection.updateOne(
                Filters.eq("_id", id),
                Updates.combine(
                        Updates.set("title", title),
                        Updates.set("author", author),
                        Updates.set("category", category),
                        Updates.set("totalCopies", totalCopies),
                        Updates.set("availableCopies", availableCopies)
                )
        );
    }

    // Update: Modify available stock copies (used by borrow/return)
    public void updateAvailableCopies(String isbn, int countChange) {
        bookCollection.updateOne(
                Filters.eq("isbn", isbn),
                Updates.inc("availableCopies", countChange)
        );
    }

    // Delete: Remove a book by its ObjectId
    public void deleteBook(ObjectId id) {
        bookCollection.deleteOne(Filters.eq("_id", id));
    }
}
