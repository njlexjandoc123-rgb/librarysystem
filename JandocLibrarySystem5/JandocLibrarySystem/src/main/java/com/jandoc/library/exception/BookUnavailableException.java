package com.jandoc.library.exception;

/** Thrown when a book has no matching record, or zero copies currently available. */
public class BookUnavailableException extends LibraryException {

    public BookUnavailableException(String isbn) {
        super("No available copies for ISBN: " + isbn);
    }
}
