package ru.falsher.jsonreader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public final class JSONArrayReader extends JSONReader implements Iterable<JSONReader> {

    private final InputStream in;

    private JSONReader currentElement = null;
    private Iterator<JSONReader> iterator = null;

    public JSONArrayReader(InputStream in, JSONReader parent) {
        super(parent);
        this.in = in;
    }

    @Override
    public void skip() throws IOException {
        if (!super.END_OF_INPUT) {
            if (currentElement != null && !currentElement.END_OF_INPUT) currentElement.skip();
            while (skip(in, JSONArrayReader.class, false));
            end_of_input(in);
        }
    }

    public JSONReader nextEntry() throws IOException {
        if (END_OF_INPUT) return null;
        if (currentElement != null && !currentElement.END_OF_INPUT) currentElement.skip();
        return currentElement = JSONReader.read(in, this);
    }

    @Override
    public Iterator<JSONReader> iterator() {
        if (iterator == null) iterator = new Iter();
        return iterator;
    }

    @Override
    public String stringify(int tabulations) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (tabulations-- >= 0) sb.append('\t');
        final String tabs = sb.toString();
        sb.setLength(0);
        sb.append("[");
        for (JSONReader entry: this) {
            if (entry == null) {
                break;
            }
            else sb.append('\n').append(tabs);
            sb
                    .append(entry.stringify(tabs.length()))
                    .append(',');
        }
        if (sb.length() != 1) {
            sb.setCharAt(sb.length() - 1, '\n');
            for (int i=tabs.length() - 1; i > 0; i--) sb.append('\t');
        }
        sb.append(']');
        return sb.toString();
    }

    private class Iter implements Iterator<JSONReader>{
        @Override
        public boolean hasNext() {
            return !JSONArrayReader.super.END_OF_INPUT;
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
