package ru.falsher.jsonreader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public final class JSONStringReader extends JSONReader {

    private final InputStream in;
    private final int traillingChar;

    private InputStream valueInputStream = null;

    public JSONStringReader(InputStream in, int traillingChar, JSONReader parent) {
        super(parent);
        this.in = in;
        this.traillingChar = traillingChar;
    }

    public InputStream getInputStream(){
        if (valueInputStream == null) valueInputStream = new valueInputStream();
        return valueInputStream;
    }

    @Override
    public void skip() throws IOException {
        if (super.END_OF_INPUT) return;
        if (parent != null) {
            if (skip(
                            in,
                            traillingChar,
                            parent.getClass(),
                            parent instanceof JSONObjectReader && (((JSONObjectReader) parent).KEY != null && ((JSONObjectReader) parent).KEY.END_OF_INPUT)
            )) parent.end_of_input(in);
        }
        else skip(in, traillingChar, null, false);
        super.END_OF_INPUT = true;
    }

    // returns a boolean hasBytes, opposite to END_OF_INPUT
    // the fourth argument is for Objects and means if the key has already been read
    protected static boolean skip(InputStream in, int traillingChar, Class<? extends JSONReader> parent, boolean readValue) throws IOException {
        boolean escapeNext = false;
        int b;
        while (true) {
            if ((b = in.read()) == -1 || b == '\n') ParseException.throwUnterminatedStringLiteral();
            else if (b == '\\') escapeNext = true;
            else {
                if (!escapeNext && b == traillingChar) {
                    if (parent != null) return skipSpaces(in, parent, parent == JSONObjectReader.class && readValue);
                    else return skipSpaces(in, null, false);
                }
                escapeNext = false;
            }
        }
    }

    @Override
    public String stringify(int tabulations) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        {
            UTF8Reader reader = new UTF8Reader(getInputStream());
            char[] chars = new char[8];
            int len = 0;
            int c;
            while ((c = reader.read()) != -1) {
                if (chars.length == len) chars = Arrays.copyOf(chars, len + 8);
                chars[len++] = (char) c;
            }
            reader.close();
            sb.append(Arrays.copyOf(chars, len));
        }
        int i;
        for (String[] s : new String[][]{new String[]{"\"","\\\""}, new String[]{"\t","\\t"}, new String[]{"\b","\\b"}, new String[]{"\n","\\n"}, new String[]{"\r","\\r"}, new String[]{"\f","\\f"}, new String[]{"\\","\\\\"}}) {
            i = 0;
            while ((i = sb.indexOf(s[0], i + 1)) > -1) sb.replace(i, i, s[1]);
        }
        sb.append('\"');
        return sb.toString();
    }

    private final class valueInputStream extends InputStream{

        @Override
        public int read() throws IOException {
            if (JSONStringReader.super.END_OF_INPUT) return -1;
            int b = in.read();
            if (b == '\\') {
                b = in.read();
                if (b == -1) {
                    ParseException.throwUnterminatedStringLiteral();
                }
                else if (b == 't') return '\t';
                else if (b == 'b') return '\b';
                else if (b == 'n') return '\n';
                else if (b == 'r') return '\r';
                else if (b == 'f') return '\f';
                else if (b == 'u') {
                    int charCode = 0;
                    for (int i = 0; i < 4; i++) {
                        b = in.read();
                        charCode *= 16;
                        if (b == -1) ParseException.throwUnterminatedStringLiteral();
                        else if (b >= '0' && b <= '9') charCode += (b - '0');
                        else if (b >= 'A' && b <= 'F') charCode += (b - 'A' + 10);
                        else if (b >= 'a' && b <= 'f') charCode += (b - 'a' + 10);
                        else ParseException.throwInvalidUnicodeEscape((char) b);
                    }
                    return charCode;
                }
                else return b;
            }
            else if (b == '\n' || b == -1) ParseException.throwUnterminatedStringLiteral();
            else if (b == traillingChar) {
                if (JSONStringReader.super.parent != null) JSONStringReader.super.parent.END_OF_INPUT = !skipSpaces(in, JSONStringReader.super.parent.getClass(), JSONStringReader.super.parent instanceof JSONObjectReader && (((JSONObjectReader) JSONStringReader.super.parent).KEY != null && ((JSONObjectReader) JSONStringReader.super.parent).KEY.END_OF_INPUT));
                else skipSpaces(in, null, false);
                END_OF_INPUT = true;
                return -1;
            } else return b;
            return -1;
        }

        @Override
        public void close() throws IOException {
            JSONStringReader.this.skip();
        }
    }
}
