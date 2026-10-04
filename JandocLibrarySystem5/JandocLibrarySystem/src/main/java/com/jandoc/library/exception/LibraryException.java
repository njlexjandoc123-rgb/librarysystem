package com.jandoc.library.exception;

/**
 * Base type for every checked exception raised by the library's service
 * layer. Being a checked exception, it forces the UI layer to explicitly
 * handle (or deliberately propagate) each business-rule violation instead
 * of relying on parsing free-text return values.
 */
public class LibraryException extends Exception {

    public LibraryException(String message) {
        super(message);
    }
}
