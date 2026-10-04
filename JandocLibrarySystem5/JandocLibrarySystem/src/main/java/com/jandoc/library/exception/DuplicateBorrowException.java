package com.jandoc.library.exception;

/** Thrown when a member already has an unreturned copy of the same ISBN checked out. */
public class DuplicateBorrowException extends LibraryException {

    public DuplicateBorrowException(String isbn) {
        super("This member already has an active borrow for ISBN: " + isbn);
    }
}
