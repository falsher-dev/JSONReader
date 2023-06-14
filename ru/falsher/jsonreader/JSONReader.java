package ru.falsher.jsonreader;

import com.sun.istack.internal.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public abstract class JSONReader{

    protected final JSONReader parent;

    protected boolean END_OF_INPUT = false;

    public JSONReader(JSONReader parent) {
        this.parent = parent;
    }

    public abstract void skip() throws IOException;

    public String stringify() throws IOException {
        return stringify(0);
    }

    protected abstract String stringify(int tabulations) throws IOException;

    protected void end_of_input(InputStream in) throws IOException {
        if (this.parent != null) {
            if (
                    !skipSpaces(
                            in,
                            this.parent.getClass(),
                            this.parent instanceof JSONObjectReader && (((JSONObjectReader) this.parent).KEY != null && ((JSONObjectReader) this.parent).KEY.END_OF_INPUT)
                    )
            ) this.parent.end_of_input(in);
        } else skipSpaces(in, null, false);
        END_OF_INPUT = true;
    }

    public static JSONReader read(InputStream in) throws IOException {
        return read(in, null);
    }

    protected static JSONReader read(InputStream in, JSONReader parent) throws IOException {
        if (parent != null && parent.END_OF_INPUT) return null;
        int i;
        while (true) {
            i = in.read();
            if (i == ' ' || i == '\n' || i == '\r' || i == 9) {
                continue;
            } else if (i == '{') {
                return new JSONObjectReader(in, parent);
            } else if (i == '[') {
                return new JSONArrayReader(in, parent);
            } else if (i == '"') {
                return new JSONStringReader(in, '"', parent);
            } else if (i == '\'') {
                return new JSONStringReader(in, '\'', parent);
            } else if ((i >= '0' && i <= '9') || i == 'x' || i == 'b' || i == '.' || i == '-') {
                final boolean isNegative = i == '-';
                if (isNegative) {
                    do {
                        i = in.read();
                    } while (i == '\n' || i == ' ' || i == '\r' || i == '\t');
                    if (!((i >= '0' && i <= '9') || i == 'x' || i == 'b' || i == '.'))
                        throw new ParseException(new NumberFormatException("Unexpected char '" + ((char) i) + "'"));
                }
                double value;
                if (i >= '1' && i <= '9') value = (i - '0');
                else value = 0;

                int numberSystem;

                if (i == 'x') numberSystem = 16;
                else if (i == 'b') numberSystem = 2;
                else numberSystem = 10;

                boolean checkNextEqXOrB;
                boolean hasFraction;
                short fractionPlace = 0;

                checkNextEqXOrB = i == '0';
                hasFraction = i == '.';

                while (true) {
                    if ((i = in.read()) == -1) break;
                    if (checkNextEqXOrB) {
                        checkNextEqXOrB = false;
                        if (i == 'x') {
                            numberSystem = 16;
                            continue;
                        } else if (i == 'b') {
                            numberSystem = 2;
                            continue;
                        } else if (i == '.') {
                            hasFraction = true;
                            continue;
                        } else {
                            numberSystem = 8;
                        }
                    }
                    if (i == ' ' || i == '\t' || i == '\r' || i == '\n') {
                        break;
                    } else if (i == ':') {
                        if (parent instanceof JSONObjectReader) break;
                    } else if (i == ',') {
                        if (parent instanceof JSONArrayReader) break;
                        else if (parent instanceof JSONObjectReader) {
                            if (((JSONObjectReader) parent).KEY != null) break;
                            else ParseException.throwUnexpectedChar(',');
                        }
                    } else if (i == ']') {
                        if (parent instanceof JSONArrayReader) {
                            parent.END_OF_INPUT = true;
                            return null;
                        } else ParseException.throwUnexpectedChar(']');
                    } else if (i == '}') {
                        if (parent instanceof JSONObjectReader) {
                            parent.END_OF_INPUT = true;
                            return null;
                        } else ParseException.throwUnexpectedChar('}');
                    } else if (i == '.') {
                        if (hasFraction) throw new ParseException(new NumberFormatException("No comma expected here"));
                        hasFraction = true;
                    } else if (i >= '0' && i <= '9') {
                        if (numberSystem == 2 && i > '1')
                            throw new ParseException(new NumberFormatException("Unexpected char '" + ((char) i) + "' in binary number"));
                        if (numberSystem == 8 && i > '7')
                            throw new ParseException(new NumberFormatException("Unexpected char '" + ((char) i) + "' in octal number"));
                        if (hasFraction) {
                            value /= numberSystem;
                            fractionPlace--;
                            value += (i - '0') * Math.pow(numberSystem, fractionPlace);
                        } else {
                            value *= numberSystem;
                            value += (i - '0');
                        }
                    } else if (numberSystem == 16) {
                        value *= 16;
                        if ((i >= 'A' && i <= 'F')) value += (i - 'A' + 10);
                        else if (i >= 'a' && i <= 'f') value += (i - 'a' + 10);
                        else
                            throw new ParseException(new NumberFormatException("Unexpected char '" + ((char) i) + "'"));
                    } else throw new ParseException(new NumberFormatException("Unexpected char '" + ((char) i) + "'"));
                }

                if (isNegative) value = -value;
                if (hasFraction) {
                    if (value > Float.MAX_VALUE && value < Float.MIN_VALUE) return new JSONNumber(value, parent);
                    else return new JSONNumber((float) value, parent);
                } else {
                    if (value > Long.MAX_VALUE || value < Long.MIN_VALUE) return new JSONNumber(value, parent);
                    else if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE)
                        return new JSONNumber((long) value, parent);
                    else return new JSONNumber((int) value, parent);
                }
            } else if (i == '}') {
                if (parent instanceof JSONObjectReader) {
                    parent.END_OF_INPUT = true;
                    return null;
                } else ParseException.throwUnexpectedChar('}');
            } else if (i == ']') {
                if (parent instanceof JSONArrayReader) {
                    parent.END_OF_INPUT = true;
                    return null;
                } else ParseException.throwUnexpectedChar(']');
            } else if (i == -1) {
                if (parent != null) ParseException.throwEndOfInput();
                else return null;
            } else {
                byte[] word = new byte[8];
                word[0] = (byte) i;
                int len = 1;
                while (true) {
                    if ((i = in.read()) == ' ' || i == '\n' || i == '\r' || i == 9) {
                        if (parent != null) parent.END_OF_INPUT = !skipSpaces(in, parent.getClass(), parent instanceof JSONObjectReader && ((JSONObjectReader) parent).KEY != null);
                        else skipSpaces(in, null, false);
                        break;
                    } else if (i == -1) {
                        if (parent == null) break;
                        else ParseException.throwEndOfInput();
                    } else if (i == ',') {
                        if (parent instanceof JSONArrayReader || (parent instanceof JSONObjectReader && ((JSONObjectReader) parent).KEY != null)) break;
                        else ParseException.throwUnexpectedChar(',');
                    } else if (i == ':') {
                        if (parent instanceof JSONObjectReader && ((JSONObjectReader) parent).KEY == null) {
                            break;
                        } else ParseException.throwUnexpectedChar(':');
                    } else if (i == '}') {
                        if (parent instanceof JSONObjectReader) {
                            parent.END_OF_INPUT = true;
                            break;
                        } else ParseException.throwUnexpectedChar('}');
                    } else if (i == ']') {
                        if (parent instanceof JSONArrayReader) {
                            parent.END_OF_INPUT = true;
                            break;
                        } else ParseException.throwUnexpectedChar(']');
                    } else {
                        if (word.length == len) word = Arrays.copyOf(word, len + 8);
                        word[len++] = (byte) i;
                    }
                }
                String w = new String(Arrays.copyOf(word, len));
                switch (w) {
                    case "true":
                    case "TRUE":
                        return JSONBoolean.TRUE;
                    case "false":
                    case "FALSE":
                        return JSONBoolean.FALSE;
                    case "null":
                    case "NULL":
                        return JSONNull.INSTANCE;
                    default:
                        ParseException.throwUnexpectedWord(w);
                }
            }
        }
    }

    // returns a boolean hasBytes, opposite to END_OF_INPUT
    static boolean skip(InputStream in, @NotNull Class<? extends JSONReader> parentElementType, boolean readValue) throws IOException {
        assert parentElementType != null;
        int i;
        while (true) {
            i = in.read();
            if (i == ' ' || i == '\n' || i == '\r' || i == 9) {
                continue;
            } else if (i == '{') {
                while (
                        skip(in, JSONObjectReader.class, false) &&
                        skip(in, JSONObjectReader.class, true)
                );
                return skipSpaces(in, parentElementType, readValue);
            } else if (i == '[') {
                while (skip(in, JSONArrayReader.class, false));
                return skipSpaces(in, parentElementType, readValue);
            } else if (i == '"' || i == '\'') {
                return JSONStringReader.skip(in, i, parentElementType, readValue);
            } else if (i == '}') {
                if (parentElementType == JSONObjectReader.class) {
                    return false;
                } else ParseException.throwUnexpectedChar('}');
            } else if (i == ']') {
                if (parentElementType == JSONArrayReader.class) {
                    return false;
                } else ParseException.throwUnexpectedChar(']');
            } else if (i == -1) {
                ParseException.throwEndOfInput();
            } else {
                if (i == '-') do {
                    i = in.read();
                } while (i == '\n' || i == ' ' || i == '\r' || i == '\t');

                do {
                    if (i == ' ' || i == '\n' || i == '\r' || i == 9) {
                        return skipSpaces(in, parentElementType, readValue);
                    } else if (i == -1) {
                        ParseException.throwEndOfInput();
                    } else if (i == ',') {
                        if (parentElementType == JSONArrayReader.class || (parentElementType == JSONObjectReader.class && readValue)) return true;
                        else ParseException.throwUnexpectedChar(',');
                    } else if (i == ':') {
                        if (parentElementType == JSONObjectReader.class && !readValue) {
                            return true;
                        } else ParseException.throwUnexpectedChar(':');
                    } else if (i == '}') {
                        if (parentElementType == JSONObjectReader.class) {
                            return false;
                        } else ParseException.throwUnexpectedChar('}');
                    } else if (i == ']') {
                        if (parentElementType == JSONArrayReader.class) {
                            return false;
                        } else ParseException.throwUnexpectedChar(']');
                    }
                    i = in.read();
                } while (true);
            }
        }
    }

    // the third argument is for Objects and means if the key has already been read
    protected static boolean skipSpaces(InputStream in, Class<? extends JSONReader> parentElementType, boolean readValue) throws IOException {
        int i;
        while (true) {
            if ((i = in.read()) == ' ' || i == '\n' || i == '\r' || i == 9) continue;
            else if (i == -1) {
                if (parentElementType == null) return false;
                else ParseException.throwEndOfInput();
            } else if (i == ',') {
                if (parentElementType == JSONArrayReader.class || (parentElementType == JSONObjectReader.class && readValue)) return true;
                else ParseException.throwUnexpectedChar(',');
            } else if (i == ':') {
                if (parentElementType == JSONObjectReader.class && !readValue) return true;
                else ParseException.throwUnexpectedChar(':');
            } else if (i == ']') {
                if (parentElementType == JSONArrayReader.class) {
                    return false;
                }
                else ParseException.throwUnexpectedChar(']');
            } else if (i == '}') {
                if (parentElementType == JSONObjectReader.class) {
                    return false;
                }
                else ParseException.throwUnexpectedChar('}');
            } else ParseException.throwUnexpectedChar((char) i);
        }
    }

}