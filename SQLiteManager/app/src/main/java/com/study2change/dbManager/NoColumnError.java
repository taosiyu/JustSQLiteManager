package com.study2change.dbManager;

public class NoColumnError extends Error {
    public String mColumnName;
    public Class mColumnType;

    public NoColumnError() {
        super();
    }

    public NoColumnError(String detailMessage) {
        super(detailMessage);
    }

    public NoColumnError(String columnName , Class columnType) {
        super("No_Such_Column_"+columnName);
        this.mColumnName = columnName;
        this.mColumnType = columnType;
    }
}
