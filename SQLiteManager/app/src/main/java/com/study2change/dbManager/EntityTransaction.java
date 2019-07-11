package com.study2change.dbManager;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class EntityTransaction {

    private final SQLiteOpenHelper dbHelper;
    private SQLiteDatabase db;
    private static final Lock lock = new ReentrantLock();

    EntityTransaction(SQLiteOpenHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

//    BEGIN EXCLUSIVE：当前事务在没有结束之前任何android中的其他线程或进程都无法对数据库进行读写操作。
//    beginTransactionNonExclusive -> BEGIN IMMEDIATE：确保android中其他线程或者进程之间读取数据不能修改数据库。
    public void begin() {
        lock.lock();
        db = dbHelper.getWritableDatabase();
        if (SQLiteOpenHelper.WAL_ENABLE) {
            db.beginTransactionNonExclusive();
        } else {
            db.beginTransaction();
        }
    }

    public void end() {
        try{
            db.endTransaction();
            db = null;
        }catch(Exception e){

        }finally{
            if ( ((ReentrantLock)lock).isHeldByCurrentThread() ) {
                lock.unlock();
            }
        }
    }

    public void commit() {
        db.setTransactionSuccessful();
    }
}
