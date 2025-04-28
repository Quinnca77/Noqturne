package org.autoTagger.exceptions;

/**
 * Thrown when a vId is found, but it has erroneous behaviour when attempting extraction.
 * This can happen because of the way YouTube handles thumbnails. This application always
 * tries to extract the highest quality cover art and goes down in quality when it can't
 * be extracted, but if none can be extracted, it throws this error.
 */
public class VIdException extends Exception {

}