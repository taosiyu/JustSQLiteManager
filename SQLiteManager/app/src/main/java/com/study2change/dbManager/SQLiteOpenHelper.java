package com.study2change.dbManager;

public class SQLiteOpenHelper {

    public static final boolean WAL_ENABLE = false;

    private android.database.sqlite.SQLiteOpenHelper helper;
    private SQLiteDatabase dbW, dbR;

    /** 一个数据库连接维护一份表名的缓存 **/
    private TableNameCache tbnCache = new TableNameCache();

    public SQLiteOpenHelper(android.database.sqlite.SQLiteOpenHelper helper) {
        this.helper = helper;
    }

    public synchronized SQLiteDatabase getWritableDatabase() {
        try{
            android.database.sqlite.SQLiteDatabase db = helper
                    .getWritableDatabase();
            if (dbW == null || dbW.db != db) {
                dbW = new SQLiteDatabase(db, tbnCache);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return dbW;
    }

    public synchronized SQLiteDatabase getReadableDatabase() {
        try {
            android.database.sqlite.SQLiteDatabase db = helper.getReadableDatabase();
            if (dbR == null || dbR.db != db) {
                dbR = new SQLiteDatabase(db, tbnCache);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dbR;
    }

    public synchronized void close() {
        helper.close();
    }

    public synchronized void stop() {
        helper = null;
    }

}
