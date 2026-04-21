package com.umlytics.enums;

public enum Visibility {
    PUBLIC("+"),
    PRIVATE("-"),
    PROTECTED("#"),
    PACKAGE("~");

    private final String symbol;

    Visibility(String symbol) { this.symbol = symbol; }

    public String getSymbol() { return symbol; }

    @Override
    public String toString() { return symbol; }
}
