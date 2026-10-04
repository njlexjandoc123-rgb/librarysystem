package com.jandoc.library.exception;

/** Thrown when returning a book that has no matching active (unreturned) borrow record. */
public class NoActiveBorrowException extends LibraryException {

    public NoActiveBorrowException(String isbn) {
        super("No active borrow record found for ISBN: " + isbn);
    }
}
