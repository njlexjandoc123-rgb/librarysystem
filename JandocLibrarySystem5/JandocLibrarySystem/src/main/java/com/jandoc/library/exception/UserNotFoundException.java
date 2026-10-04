package com.jandoc.library.exception;

/** Thrown when a borrow/return operation references a member that doesn't exist. */
public class UserNotFoundException extends LibraryException {

    public UserNotFoundException(String userId) {
        super("No member found with id: " + userId);
    }
}
