package ru.falsher.jsonreader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public final class JSONObjectReader extends JSONReader implements Iterable<JSONReader> {

    private final InputStream in;
    protected JSONReader KEY = null;
    private JSONReader VALUE = null;

    private Iterator<JSONReader> iter = null;

    public JSONObjectReader(InputStream in, JSONReader parent) {
        super(parent);
        this.in = in;
    }

    @Override
    public void skip() throws IOException {
        if (!super.END_OF_INPUT) {
            if (KEY != null && !KEY.END_OF_INPUT) {
                KEY.skip();
                if (!skip(in, JSONObjectReader.class, true)) {
                    super.END_OF_INPUT = true;
                    return;
                }
            }
            while (
                    skip(in, JSONObjectReader.class, false) &&
                    skip(in, JSONObjectReader.class, true)
            );
            end_of_input(in);
        }
    }

    public JSONReader nextEntry() throws IOException {
        if (END_OF_INPUT) return null;
        if (KEY != null) {
            if (!KEY.END_OF_INPUT) KEY.skip();
            if (VALUE != null) {
                if (!VALUE.END_OF_INPUT) {
                    VALUE.skip();
                    if (END_OF_INPUT) return null;
                }
                VALUE = null;
            } else if (END_OF_INPUT || (END_OF_INPUT = !JSONReader.skip(in, JSONObjectReader.class, true))) return null;
            KEY = null;
        }
        return KEY = JSONReader.read(in, this);
    }

    public JSONReader value() throws IOException {
        if (VALUE == null) {
            if (KEY == null) return null;
            if (!KEY.END_OF_INPUT) KEY.skip();
            if (!END_OF_INPUT) {
                VALUE = JSONReader.read(in, this);
            }
        }
        return VALUE;
    }

    @Override
    public Iterator<JSONReader> iterator() {
        if (iter == null) iter = new keyIterator();
        return iter;
    }

    @Override
    public String stringify(int tabulations) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (tabulations-- >= 0) sb.append('\t');
        final String tabs = sb.toString();
        sb.setLength(0);
        sb.append("{");
        for (JSONReader key: this) {
            if (key == null) {
                break;
            }
            else sb.append('\n').append(tabs);
            sb
                    .append(key.stringify(tabs.length()))
                    .append(": ");
            if (value() == null) sb.append("null");
            else sb.append(VALUE.stringify(tabs.length()));
            sb.append(',');
        }
        if (sb.length() != 1) {
            sb.setCharAt(sb.length() - 1, '\n');
            for (int i=tabs.length() - 1; i > 0; i--) sb.append('\t');
        }
        sb.append('}');
        return sb.toString();
    }

    private class keyIterator implements Iterator<JSONReader> {

        @Override
        public boolean hasNext() {
            return !END_OF_INPUT;
        }

        @Override
        public JSONReader next() {
            try {
                return nextEntry();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
