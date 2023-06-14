package ru.falsher.jsonreader;

import java.io.IOException;

public class ParseException extends IOException {

    public ParseException() {}

    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseException(Throwable cause) {
        super(cause);
    }

    public static void throwEndOfInput() throws ParseException {
        throw new ParseException("Unexpected end of input");
    }

    public static void throwUnexpectedChar(char c) throws ParseException {
        throw new ParseException("Unexpected character '"+c+"'");
    }

    public static void throwUnexpectedWord(String word) throws ParseException {
        throw new ParseException("Unexpected word '"+word+"'");
    }

    public static void throwUnterminatedStringLiteral() throws ParseException {
        throw new ParseException("Unterminated String Literal");
    }

    public static void throwInvalidUnicodeEscape(char c) throws ParseException {
        throw new ParseException("Invalid Unicode escape sequence. Char '"+c+"'");
    }

}
