package ru.falsher.jsonreader;

public final class JSONNull extends JSONReader {

    public static final JSONNull INSTANCE = new JSONNull();

    public JSONNull() {
        super(null);
    }

    @Override
    public void skip(){}

    @Override
    protected String stringify(int tabulations) throws IOException {
        return "null";
    }

}
