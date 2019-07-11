package com.study2change.dbManager;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQLiteDatabase {
    private static final String TAG = "db";
    final android.database.sqlite.SQLiteDatabase db;
    final static String SQL_GET_TABLE_ATTR = "select sql from sqlite_master where type=? and name=?";
    private final Map<String, ArrayList<String>> tableMap = new HashMap<String, ArrayList<String>>();
    private final Map<String, ArrayList<String>> queryCacheMap = new HashMap<String, ArrayList<String>>();
    private TableNameCache tableNameCache = null;

    SQLiteDatabase(android.database.sqlite.SQLiteDatabase db, TableNameCache cache) {
        this.db = db;
        this.tableNameCache = cache;
    }

    public void beginTransaction() {
        try {
            db.beginTransaction();
        } catch (Throwable t) {
            handleDBErr(t);
        }
    }

    @TargetApi(11)
    public void beginTransactionNonExclusive() {
        long beginTime = System.currentTimeMillis();
        try {
            db.beginTransactionNonExclusive();
        } catch (Throwable t) {
            handleDBErr(t);
        }
    }

    public void endTransaction() {
        try {
            db.endTransaction();
        } catch (Throwable t) {
            handleDBErr(t);
        }
    }

    public void setTransactionSuccessful() {
        try {
            db.setTransactionSuccessful();
        } catch (Throwable t) {
            handleDBErr(t);
        }
    }

    public void close() {
        try {
            db.close();
        } catch (Throwable t) {
            handleDBErr(t);
        }
    }

    public int delete(String table, String whereClause, String[] whereArgs) {

        convertWhereValues(table, whereClause, whereArgs);
        try {
            int count =  db.delete(table, whereClause, whereArgs);
            return count;
        } catch (Throwable t) {
            handleDBErr(t);
        }
        return -1;
    }

    public long insert(String table, String nullColumnHack, ContentValues values) {
        values = convertContentValues(table, values);
        try {
            long result = db.insert(table, nullColumnHack, values);
            return result;
        } catch (Throwable t) {
            handleDBErr(t);
        }
        return -1;
    }

    public long replace(String table, String nullColumnHack,
                        ContentValues initialValues) {
        initialValues = convertContentValues(table, initialValues);
        try {
            long result = db.replace(table, nullColumnHack, initialValues);
            return result;
        } catch (Throwable t) {
            handleDBErr(t);
        }
        return -1;
    }

    public int update(String table, ContentValues values, String whereClause,
                      String[] whereArgs) {
        values = convertContentValues(table, values);
        convertWhereValues(table, whereClause, whereArgs);
        try {
            int result = db.update(table, values, whereClause, whereArgs);
            return result;
        } catch (Throwable t) {
            handleDBErr(t);
        }
        return -1;
    }

    public int count(String table, String whereClause, String[] whereArgs) {
        String sql = "select count(*) from " + table;
        if (whereClause != null && whereArgs != null) {
            convertWhereValues(table, whereClause, whereArgs);
            sql += " where " + whereClause;
        }
        Cursor c = rawQuery(sql, whereArgs);
        int count = 0;
        if (c != null) {
            c.moveToFirst();
            count = c.getInt(0);
            c.close();
        }
        return count;
    }

    public boolean execSQL(String sql, Object[] bindArgs) {
        try {
            db.execSQL(sql, bindArgs);
            return true;
        } catch (Throwable t) {
            handleDBErr(t);
            return false;
        }
    }

    public boolean execSQL(String sql) {
        try {
            db.execSQL(sql);
            return true;
        } catch (Throwable t) {
            handleDBErr(t);
            return false;
        }
    }

    public Cursor rawQuery(String sql, String[] selectionArgs) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(sql, selectionArgs);
        } catch (Throwable t) {
            handleDBErr(t);
        }finally {

        }
        return cursor;
    }

    public Cursor rawQuery(String sql, String table, String selection, String[] selectionArgs) {
        convertWhereValues(table, selection, selectionArgs);
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(sql, selectionArgs);
        } catch(Throwable t){
            handleDBErr(t);
        }
        finally {

        }

        return cursor;
    }

    private Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs,
                         String groupBy, String having, String orderBy, String limit) {

        convertWhereValues(table, selection, selectionArgs);
        Cursor cursor = null;
        try {
            cursor = db.query(false, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
        } catch (Throwable t) {
            handleDBErr(t);
        }finally {

        }
        return cursor;
    }

    public Cursor query(String table, String selection, String[] selectionArgs) {
        return query(false, table, null, selection, selectionArgs, null, null, null, null);
    }

    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String orderBy, String limit) {
        return query(false, table, columns, selection, selectionArgs, null, null, orderBy, limit);
    }

    public String[] getAllTableNameFromCache() {
        buildTableNameCache();
        return tableNameCache.getAllTableNames();
    }

    public String[] getAllTableNameFromDB() {
        String[] tbs = null;
        String sql = "select distinct tbl_name from Sqlite_master";

        if(db != null) {
            Cursor c = db.rawQuery(sql, null);
            int index = 0;
            if (c != null && c.moveToFirst()) {
                tbs = new String[c.getCount()];
                do {
                    String tn = c.getString(0);
                    tbs[index++] = tn;
                } while (c.moveToNext());
            }
            if (c != null)
            {
                c.close();
            }
        }

        return tbs;
    }

    public void addToTableCache(String tableName) {

        tableNameCache.addToTableNameCache(tableName);
    }

    public void removeFromTableCache(String tableName) {

        tableNameCache.deleteFromTableCache(tableName);
    }

    public boolean containsTable(String tableName) {

        buildTableNameCache();
        return tableNameCache.isContainsTableInCache(tableName);
    }

    //===================================== private

    /**
     * 创建数据库表名的缓存:如果缓存已建立，则什么都不会做
     */
    private void buildTableNameCache() {
        if(!tableNameCache.isInit) {
            try{
                String[] tbs = getAllTableNameFromDB();
                tableNameCache.initTableCache(tbs);
            }catch(Exception e){
                //初始化缓存异常
            }
        }
    }

    private void convertWhereValues(String table, String whereClause,
                                    String[] whereArgs) {
        if (whereClause == null || whereArgs == null)
            return;
        ArrayList<String> nameKeys = getTableInfo(table);
        if (nameKeys != null) {
            ArrayList<String> whereNameKeys = analyseRawQueryWhere(whereClause);
            for (int i = 0; i < whereNameKeys.size(); i++) {
                if (nameKeys.contains(whereNameKeys.get(i)))
                    if(whereArgs[i] instanceof String){
                        whereArgs[i] = convertStr(whereArgs[i]);
                    }
            }
        }
    }

    private ContentValues convertContentValues(String table,
                                               ContentValues values) {
        if (values == null || values.size() <= 0)
            return values;
        ContentValues retVal = new ContentValues(values);
        ArrayList<String> nameKeys = getTableInfo(table);
        if (nameKeys != null) {
            for (String key : nameKeys) {
                if (values.containsKey(key)) {
                    Object value = values.get(key);
                    if(value instanceof String){
                        String s = (String) values.get(key);
                        if (s != null && s.length() > 0)
                            retVal.put(key, convertStr(s));
                    }else if(value instanceof byte[]){
                        retVal.put(key, (byte[])value);
                    }
                }
            }
        }
        return retVal;
    }

    private ArrayList<String> getTableInfo(String table) {
        if (!tableMap.containsKey(table)) {
            Cursor cursor = rawQuery(SQL_GET_TABLE_ATTR, new String[] {"table", table });
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    ArrayList<String> nameKeys = analyseTableField(cursor.getString(0), new String[]{"TEXT","BLOB"});
                    // if (nameKeys.size() > 0 )
                    tableMap.put(table, nameKeys);
                    // }
                }
                cursor.close();
            }
        }
        return tableMap.get(table);
    }

    private ArrayList<String> analyseTableField(String tableSql, String[] typeKeys) {
        try {
            int start = tableSql.indexOf("(");
            int end = tableSql.indexOf(")");
            tableSql = tableSql.substring(start + 1, end);
            String[] fields = tableSql.split(",");
            ArrayList<String> nameKeys = new ArrayList<String>();
            for (String typeKey : typeKeys) {
                typeKey = typeKey.toLowerCase();
                for (String string : fields) {
                    string = string.trim();
                    String[] s = string.split(" ");
                    if (s.length > 1 && typeKey.equals(s[1].toLowerCase()))
                        nameKeys.add(s[0]);
                }
            }
            return nameKeys;
        } catch ( Exception e ) {
            return new ArrayList<String>();
        }
    }

    private ArrayList<String> analyseRawQueryWhere(String queryStatement) {
        // Pattern p = Pattern.compile("from\\s(\\s*\\w+\\s*,)*\\s*\\w+\\s+");
        if (queryCacheMap.containsKey(queryStatement))
            return queryCacheMap.get(queryStatement);

        Pattern p = Pattern
                .compile("\\s*\\w+\\s*(>|<|=|>=|<=|!=|=!|<>)\\s*\\?\\s*");
        Matcher m = p.matcher(queryStatement);
        ArrayList<String> nameKeys = new ArrayList<String>();

        while (m.find()) {
            String sss = m.group().trim();
            // System.out.println(sss);
            Pattern p2 = Pattern.compile("\\w+");
            Matcher m2 = p2.matcher(sss);
            m2.find();
            nameKeys.add(m2.group());
        }
        queryCacheMap.put(queryStatement, nameKeys);
        return nameKeys;
    }

    private String convertStr(Object object) {
        if (object == null)
            return null;
        String value = object.toString();
        return value;
    }


    private void handleDBErr(Throwable t) {
            if( t.getMessage()!=null && ! t.getMessage().contains("no such table") ){
                StringBuilder err = new StringBuilder();
                Log.e(TAG, "handleDBErr: SQLiteDatabase" + err,t);
            }
        if(t.getMessage() != null && t.getMessage().contains("cannot start a transaction")){
            //"transaction exception!"
        }
    }


}
