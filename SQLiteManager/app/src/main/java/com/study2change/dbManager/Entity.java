package com.study2change.dbManager;

import android.database.Cursor;

public abstract class Entity {

    public static final int NEW = 991000;
    public static final int MANAGED = 991001;
    public static final int DETACHED = 991002;
    public static final int REMOVED = 991003;

    int _status = NEW;
    @notColumn
    long _id = -1;

    public long getId() {
        return _id;
    }

    public int getStatus() {
        return _status;
    }

    public void setId(long id) {
        _id = id;
    }

    public void setStatus(int status) {
        _status = status;
    }

    public String getTableName()
    {
        return getClass().getSimpleName();
    }

    /**
     * 此方法与上面方法是对应的
     * @return
     */
    protected Class<? extends Entity> getClassForTable()
    {
        return this.getClass();
    }

    protected boolean entityByCursor(Cursor cursor){
        return false;
    }

    /**
     * 在从DB读取完成之后调用
     */
    protected void postRead()
    {

    }
    /**
     * 从DB写之前调用
     */
    protected void prewrite()
    {

    }
    /**
     * 从DB写之后调用
     */
    protected void postwrite()
    {

    }
}
