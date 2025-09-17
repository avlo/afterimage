package com.prosilion.afterimage;

public class MissingIdentifierTagException extends RuntimeException {
  public static final String message = "Event missing required IdentifierTag";

  public MissingIdentifierTagException() {
    super(message);
  }
}
