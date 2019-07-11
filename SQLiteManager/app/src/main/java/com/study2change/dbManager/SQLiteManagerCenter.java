package com.study2change.dbManager;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQuery;
import android.nfc.Tag;
import android.text.TextUtils;
import android.util.Log;

import com.study2change.dbManager.SQLiteOpenHelper;
import com.sutdy2change.sqlitemanager.DemoEntity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SQLiteManagerCenter {

    private static final String CLOSE_EXCEPTION_MSG = "The SQLiteManagerCenter has been already closed";
    private boolean closed;
    private SQLiteOpenHelper dbHelper;

    private static final int DB_VERSION = 157;
    protected int dbVersion = -1;

    public static Context mContext;//请在此指定context

    protected SQLiteOpenHelperImpl mInnerDbHelper;

    public SQLiteManagerCenter(String name,Context context) {
        mContext = context;
        dbHelper = build(name);
    }

    public EntityManager createEntityManager() {
        if (closed) {
            throw new IllegalStateException(CLOSE_EXCEPTION_MSG);
        }
        EntityManager em = new EntityManager(dbHelper);
        closed = false;
        return em;
    }

    public SQLiteOpenHelper build(String name) {
        if (dbHelper == null) {
            int version = dbVersion <= 0 ? DB_VERSION : dbVersion;
            mInnerDbHelper = new SQLiteOpenHelperImpl(name + ".db", null,version);
            dbHelper = new SQLiteOpenHelper(mInnerDbHelper);
        }
        return dbHelper;
    }

    //创建表格用
    protected void createDatabase(SQLiteDatabase db) {
        try {
            db.execSQL(TableBuilder.createSQLStatement(new DemoEntity()));
        }catch (SQLException e){
            e.printStackTrace();
        }

    }

    //升级用
    protected void upgradeDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {

    }


    protected class SQLiteOpenHelperImpl extends android.database.sqlite.SQLiteOpenHelper {

        private String databaseName;
        private SQLiteDatabase dbR, dbW;
        private SQLiteDatabase mInnerDb;

        public SQLiteOpenHelperImpl(String name, CursorFactory factory, int version) {
            super(mContext, name, new Factory(), version);
            databaseName = name;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            createDatabase(db);

        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            super.onOpen(db);

            mInnerDb = db;

            // hack方式
            // 获取mConfigurationLocked，修改maxSqlCacheSize
            // 获取mConnectionPoolLocked，调用reconfigure()，传入修改后mConfigurationLocked
            int MAX_SIZE = 150;
            Class<?> cls = SQLiteDatabase.class;
            try {
                Field config = cls.getDeclaredField("mConfigurationLocked");
                config.setAccessible(true);
                // mConfigurationLocked
                Object configObject = config.get(db);
                // maxSqlCacheSize
                Field size = configObject.getClass().getDeclaredField("maxSqlCacheSize");
                size.setAccessible(true);
                // 关键，设置LRU MAX SIZE
                size.set(configObject, MAX_SIZE);

                Field pool = cls.getDeclaredField("mConnectionPoolLocked");
                pool.setAccessible(true);
                // mConnectionPoolLocked
                Object poolObject = pool.get(db);
                Method reconfigure = null;
                Method[] methods = poolObject.getClass().getDeclaredMethods();
                for (Method method : methods) {
                    if (TextUtils.equals(method.getName(), "reconfigure")) {
                        // reconfigure()
                        reconfigure = method;
                        break;
                    }
                }
                if (reconfigure != null) {
                    reconfigure.setAccessible(true);
                    // hack
                    reconfigure.invoke(poolObject, configObject);

                } else {

                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

        }

        public void dropAllTable() {
            dropAllTable(mInnerDb);
        }

        private void dropAllTable(SQLiteDatabase db) {
            String[] tables = getAllTableName(db);
            if (tables != null) {
                for (String tb : tables) {
                    try {
                        db.execSQL(TableBuilder.dropSQLStatement(tb));
                    } catch ( SQLiteException e ) {
                        e.printStackTrace();
                    }
                }
            }

            onCreate(db);
        }

        private String[] getAllTableName(SQLiteDatabase db) {
            String sql = "select distinct tbl_name from Sqlite_master";
            Cursor c = null;
            String[] tbs = null;
            try {
                c = db.rawQuery(sql, null);
                int index = 0;
                if (c != null && c.moveToFirst()) {
                    tbs = new String[c.getCount()];
                    do {
                        String tn = c.getString(0);
                        tbs[index++] = tn;
                    } while (c.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (c != null){
                    c.close();
                    c = null;
                }
            }

            return tbs;
        }

        @Override
        public void onConfigure(SQLiteDatabase db) {
            super.onConfigure(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            upgradeDatabase(db, oldVersion, newVersion);
        }

        @Override
        public synchronized void close() {
            super.close();
        }

        @Override
        public synchronized SQLiteDatabase getWritableDatabase() {
            try{
                dbW = super.getWritableDatabase();
                dbW.setLockingEnabled(false);
            } catch(Exception e){
                e.printStackTrace();
            }
            return dbW;
        }

        @Override
        public synchronized SQLiteDatabase getReadableDatabase() {
            try{
                dbR = super.getReadableDatabase();
            } catch(Exception e) {
                e.printStackTrace();
            }
            return dbR;
        }


    }


    private class Factory implements CursorFactory {

        @Override
        public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query) {
            return new SQLiteCursor(masterQuery,editTable,query) {
                @Override
                public String getString(int columnIndex) {
                    String str = super.getString(columnIndex);
                    return str;
                }

                @Override
                public byte[] getBlob(int columnIndex) {
                    byte[] b = super.getBlob(columnIndex);
                    return b;
                }
            };
        }
    }
}
