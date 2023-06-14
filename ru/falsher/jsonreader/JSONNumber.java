package ru.falsher.jsonreader;

public final class JSONNumber extends JSONReader {

    private final Number value;

    public JSONNumber(Number value, JSONReader parent) {
        super(parent);
        this.value = value;
        super.END_OF_INPUT = true;
    }

    @Override
    public void skip() {}

    @Override
    protected String stringify(int tabulations) throws IOException {
        return value.toString();
    }

    public int intValue() {
        return value.intValue();
    }

    public long longValue() {
        return value.longValue();
    }

    public float floatValue() {
        return value.floatValue();
    }

    public double doubleValue() {
        return value.doubleValue();
    }

}
