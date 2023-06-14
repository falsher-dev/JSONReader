package ru.falsher.jsonreader;

public final class JSONBoolean extends JSONReader {

    public static final JSONBoolean
            TRUE  = new JSONBoolean(true),
            FALSE = new JSONBoolean(false);

    public final boolean value;

    public JSONBoolean(boolean value) {
        super(null);
        this.value = value;
    }

    @Override
    public void skip() {}

    @Override
    protected String stringify(int tabulations) throws IOException {
        return toString();
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }
}
