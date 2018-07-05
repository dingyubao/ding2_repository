package org.onosproject.mongodb;

public class DBException extends Exception {
    public DBException() {
    }

    public DBException(String var1) {
        super(var1);
    }

    public DBException(String var1, Throwable var2) {
        super(var1, var2);
    }

    public DBException(Throwable var1) {
        super(var1);
    }
}
