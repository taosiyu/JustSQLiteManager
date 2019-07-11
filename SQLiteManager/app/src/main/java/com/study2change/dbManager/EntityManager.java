package com.study2change.dbManager;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class EntityManager {

    private static final String SQL = "EntityManager";
    SQLiteOpenHelper dbHelper;
    private EntityTransaction transaction;

    private static final Hashtable<String, Boolean> createTableCache = new Hashtable<String, Boolean>();

    SQLiteDatabase db;

    EntityManager(SQLiteOpenHelper dbHelper) {
        this.dbHelper = dbHelper;
    }


    //=========================================================================================删除
    public boolean drop(Class<? extends Entity> clazz) {

        if (db == null) {
            db = dbHelper.getWritableDatabase();
        }
        try {
            String tableName = TableBuilder.getTableName(clazz);
            db.execSQL("DROP TABLE IF EXISTS " + tableName);
            createTableCache.remove(tableName);
            db.removeFromTableCache(tableName);
            return true;
        } catch (Exception e) {

        }

        return false;
    }

    public boolean drop(String tableName) {

        if (db == null) {
            db = dbHelper.getWritableDatabase();
        }
        try {
            db.execSQL("DROP TABLE IF EXISTS " + tableName);
            createTableCache.remove(tableName);
            db.removeFromTableCache(tableName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    //=========================================================================================查
    public Entity find(Class<? extends Entity> clazz, String unique) {
        Entity entity = null;
        List<Field> f = TableBuilder.getValidField(clazz);
        Field field = null;
        for (int fI = 0, fLen = f.size(); fI < fLen; fI ++) {
            field = f.get(fI);
            if (field.isAnnotationPresent(unique.class)) {
                String fieldName = field.getName();
                String selection = fieldName + "=?";

                List<? extends Entity> list = null;
                try {
                    list = queryInner(clazz,  TableBuilder.getTableName(clazz), false,
                            selection, new String[] { unique } , null, null, null, null, null);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                if (list != null && list.size() > 0) {
                    entity = list.get(0);
                }
                break;
            }
        }
        return entity;
    }

    public Entity find(Class<? extends Entity> clazz, String... unique) {
        if (!clazz.isAnnotationPresent(uniqueConstraints.class)) {
            throw new IllegalStateException(
                    "No uniqueConstraints annotation in the Entity "
                            + clazz.getSimpleName());
        }
        Entity entity = null;
        String constraints = "";
        try {
            constraints = clazz.getAnnotation(uniqueConstraints.class).columnNames();
        } catch ( IncompatibleClassChangeError e ) {
            e.printStackTrace();
            return entity;
        }
        String[] selection = constraints.split(",");
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < selection.length; i++) {
            sb.append(selection[i]);
            if (i == selection.length - 1) {
                sb.append("=?");
            } else {
                sb.append("=? and ");
            }
        }

        List<? extends Entity> list = null;
        try {
            list = queryInner(clazz,  TableBuilder.getTableName(clazz), false,
                    sb.toString(), unique, null, null, null, null, null);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if (list != null) {
            entity = list.get(0);
        }
        return entity;
    }

    public Entity find(Class<? extends Entity> clazz, long _id) {

        List<? extends Entity> list = null;
        try {
            list = queryInner(clazz,  TableBuilder.getTableName(clazz),
                    false, TableBuilder.PRIMARY_KEY + "=?", new String[] { String.valueOf(_id) }, null, null, null, null, null);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if (list != null) {
            return list.get(0);
        } else {
            return null;
        }
    }

    public Entity find(Class<? extends Entity> clazz, String selection, String[] selectionArgs) {
        List<? extends Entity> list = query(clazz, true, selection, selectionArgs, null, null, null, "1");
        if (list != null) {
            return list.get(0);
        }
        return null;
    }

    public List<? extends Entity> query(Class<? extends Entity> clazz) {
        String table;
        try {
            table = TableBuilder.getTableName(clazz);
            return queryInner(clazz, table, false, null, null, null, null, null, null, null);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Cursor query(boolean distinct, String table, String[] columns,
                        String selection, String[] selectionArgs, String groupBy,
                        String having, String orderBy, String limit) {
        SQLiteDatabase db = null;
        try {
            boolean exist = tabbleIsExist(table);
            if (exist) {
                db = dbHelper.getReadableDatabase();
                return db.query(table, columns, selection, selectionArgs, orderBy, limit);
            }
        } catch (Exception e){

        } finally {
        }
        return null;
    }

    public List<? extends Entity> query(Class<? extends Entity> clazz,
                                        String table, boolean distinct, String selection,
                                        String[] selectionArgs, String groupBy, String having,
                                        String orderBy, String limit) {
        return query(clazz, table, distinct, selection, selectionArgs, groupBy, having, orderBy, limit, null);
    }

    public List<? extends Entity> query(Class<? extends Entity> clazz,
                                        String table, boolean distinct, String selection,
                                        String[] selectionArgs, String groupBy, String having,
                                        String orderBy, String limit,NoColumnErrorHandler handler) {
        return queryInner(clazz, table, distinct, selection, selectionArgs, groupBy, having, orderBy, limit, handler);
    }

    public List<? extends Entity> query(Class<? extends Entity> clazz,
                                        boolean distinct, String selection, String[] selectionArgs,
                                        String groupBy, String having, String orderBy, String limit) {

        try {
            String table = TableBuilder.getTableName(clazz);
            return query(clazz, table, distinct, selection, selectionArgs,
                    groupBy, having, orderBy, limit);
        } catch (Exception e) {

        }
        return null;
    }

    public List<? extends Entity> rawQuery(Class<? extends Entity> clazz,
                                           String sql, String[] selectionArgs) {

        List<? extends Entity> list = null;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery(sql, selectionArgs);
            if (cursor != null) {
//				cursor = new CursorOpt(cursor);  rawQuery可能是非单表操作
                list = cursor2List(clazz, null, cursor);
            }
        } catch (Exception e) {

        } catch (OutOfMemoryError oom) {

        } finally {
            if(null != cursor){
                cursor.close();
                cursor = null;
            }
        }

        return list;
    }

    public List<? extends Entity> rawQuery(Class<? extends Entity> clazz, String sql, String table, String selection, String[] selectionArgs) {
        List<? extends Entity> list = null;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery(sql, table, selection, selectionArgs);
            if (cursor != null) {
                list = cursor2List(clazz, null, cursor);
            }
        } catch (Exception e) {

        } finally {
            if (null != cursor) {
                cursor.close();
                cursor = null;
            }
        }

        return list;
    }

    public boolean tabbleIsExist(String tableName) {
        if (tableName == null) {
            return false;
        }
        if ("Sqlite_master".equalsIgnoreCase(tableName)) {
            return true;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (db == null) {
            return false;
        }

        boolean isExist = db.containsTable(tableName);

        return isExist;
    }

    protected ContentValues createContentValue(Entity entity)
            throws IllegalArgumentException, IllegalAccessException {

        List<Field> f = TableBuilder.getValidField(entity.getClassForTable());
        int size = f.size();
        Field field = null;
        ContentValues cv = new ContentValues(size);
        for (int i = 0; i < size; i ++) {
            field = f.get(i);

            String name = field.getName();
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            Object value = field.get(entity);
            if (value instanceof Integer) {
                cv.put(name, (Integer) value);
            } else if (value instanceof Long) {
                cv.put(name, (Long) value);
            } else if (value instanceof String) {
                cv.put(name, (String) value);
            } else if (value instanceof byte[]) {
                cv.put(name, (byte[]) value);
            } else if (value instanceof Short) {
                cv.put(name, (Short) value);
            } else if (value instanceof Boolean) {
                cv.put(name, (Boolean) value);
            } else if (value instanceof Double) {
                cv.put(name, (Double) value);
            } else if (value instanceof Float) {
                cv.put(name, (Float) value);
            } else if (value instanceof Byte) {
                cv.put(name, (Byte) value);
            } else if (value instanceof Boolean) {
                cv.put(name, (Boolean) value);
            }
        }

        return cv;
    }

    public Entity cursor2Entity(Class<? extends Entity> clazz, Cursor cursor) {
        try {
            return cursor2Entity(clazz, TableBuilder.getTableName(clazz),
                    cursor);
        } catch (Exception e) {
            return null;
        }
    }

    public Entity cursor2Entity(Class<? extends Entity> clazz,
                                String tableName,Cursor cursor) {
        return cursor2Entity(clazz, tableName, cursor, null);
    }

    public Entity cursor2Entity(Class<? extends Entity> clazz,
                                String tableName, Cursor cursor,NoColumnErrorHandler handler) {

        if (cursor.isBeforeFirst()) {
            cursor.moveToFirst();
        }
        long _id = -1;
        try {
            if(cursor.getColumnIndex(TableBuilder.PRIMARY_KEY) >= 0){
                _id = cursor.getLong(cursor.getColumnIndex(TableBuilder.PRIMARY_KEY));
            }
        } catch (Exception e1) {
        }
        Entity entity = null;
        try {
            entity = clazz.newInstance();
            if(entity != null){
                entity._id = _id;

                boolean result = entity.entityByCursor(cursor);

                if(!result){
                    List<Field> fields = TableBuilder.getValidField(entity);

                    Field field;
                    for (int fI = 0, fLen = fields.size(); fI < fLen; fI++) {
                        field = fields.get(fI);

                        Class type = field.getType();
                        if (Entity.class.isAssignableFrom(type)) {// 级联查询
                            Entity memberEntity = cursor2Entity(type, cursor);
                            memberEntity._status = Entity.DETACHED;
                            field.set(entity, memberEntity);
                        }
                        String columnName = field.getName();
                        int columnIndex = cursor.getColumnIndex(columnName);
                        if (columnIndex != -1) {
                            if (!field.isAccessible()) {
                                field.setAccessible(true);
                            }
                            if (type == long.class) {
                                field.set(entity, cursor.getLong(columnIndex));
                            } else if (type == int.class) {
                                field.set(entity, cursor.getInt(columnIndex));
                            } else if (type == String.class) {
                                field.set(entity, cursor.getString(columnIndex));
                            } else if (type == byte.class) {
                                field.set(entity, (byte) cursor.getShort(columnIndex));
                            } else if (type == byte[].class) {
                                field.set(entity, cursor.getBlob(columnIndex));
                            } else if (type == short.class) {
                                field.set(entity, cursor.getShort(columnIndex));
                            } else if (type == boolean.class) {
                                field.set(entity, cursor.getInt(columnIndex) != 0);
                            } else if (type == float.class) {
                                field.set(entity, cursor.getFloat(columnIndex));
                            } else if (type == double.class) {
                                field.set(entity, cursor.getDouble(columnIndex));
                            }
                        }else{
                            if(handler != null){
                                handler.handleNoColumnError(new NoColumnError(columnName,type));
                            }
                        }
                    }

                }

                if (_id != -1 && tableName != null) {
                    entity._status = Entity.MANAGED;
                } else {
                    entity._status = Entity.DETACHED;
                }

                entity.postRead();

            }
        } catch (Exception e) {

            entity = null;
        }

        return entity;
    }

    protected List<? extends Entity> cursor2List(Class<? extends Entity> clazz,
                                                 String tableName, Cursor cursor) {
        return cursor2List(clazz, tableName, cursor, null);
    }

    protected List<? extends Entity> cursor2List(Class<? extends Entity> clazz,
                                                 String tableName, Cursor cursor,NoColumnErrorHandler handler) {

        List<Entity> list = null;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getCount();
                do {
                    Entity entity = cursor2Entity(clazz, tableName, cursor,handler);
                    if (entity != null) {
                        if (list == null) {
                            try {
                                list = new ArrayList<Entity>(count);
                            } catch (Throwable t) {
                                // rdm上报OOM,跳过Add步骤
                                continue;
                            }
                        }
                        list.add(entity);


                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e1) {

        }


        return list;
    }

    public boolean execSQL(String sql) {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getReadableDatabase();
            db.execSQL(sql);
        } catch (Exception e) {
            return false;
        } finally {
        }
        return true;
    }

    public void persist(Entity entity) {
            try {
                insertOrReplace(entity, false);
            } catch (Throwable t) {
            }
    }

    public void persistOrReplace(Entity entity) {

            try {
                insertOrReplace(entity, true);
            } catch (Throwable t) {
            }

    }

    protected void insertOrReplace(Entity entity, boolean isReplace) {

        if (db == null) {
            db = dbHelper.getWritableDatabase();
        }
        if (entity._status == Entity.NEW) {
            String table = entity.getTableName();
            createTable(table, entity, false);
            entity.prewrite();
            try {
                ContentValues cv = createContentValue(entity);
                long _id = -1;
                if (isReplace) {
                    _id = db.replace(table, null, cv);
                } else {
                    _id = db.insert(table, null, cv);
                }

                if (_id == -1) {
                    // 插入或替换数据失败，可能没有创建表，先强制创建表，然后在执行插入或替换数据 (这一步绝大部分情况下不会执行,预防万一用)
                    boolean isCreate = createTable(table, entity, true);
                    if(isCreate){
                        if (isReplace) {
                            _id = db.replace(table, null, cv);
                        } else {
                            _id = db.insert(table, null, cv);
                        }
                    }
                }

                if (_id != -1) {
                    entity._id = _id;
                    entity._status = Entity.MANAGED;
                } else {

                }

            } catch (Exception e) {

            }
            entity.postwrite();
        }
    }

    public boolean update(Entity entity) {

        if (db == null) {
            db = dbHelper.getWritableDatabase();
        }
        entity.prewrite();
        try {
            if (entity._status == Entity.MANAGED||entity._status == Entity.DETACHED) {
                ContentValues cv = createContentValue(entity);
                return db.update(entity.getTableName(), cv, "_id=?",
                        new String[] { String.valueOf(entity._id) }) > 0;
            }
        } catch (Exception e) {

        }
        entity.postwrite();

        return false;
    }
    public boolean update(String table, ContentValues values, String whereClause,
                          String[] whereArgs){

        try {
            db = dbHelper.getWritableDatabase();
            return db.update(table, values, whereClause, whereArgs) > 0;
        } catch (Exception e) {

        }
        return false;

    }
    public boolean update(String sql) {
        return update(sql, null);
    }
    public boolean update(String sql, Object[] args) {

        try {
            db = dbHelper.getWritableDatabase();
            if (args == null){
                db.execSQL(sql);
            }else {
                db.execSQL(sql, args);
            }
        } catch (Exception e) {

        }
        return false;
    }

    public boolean remove(Entity entity) {

        boolean success = false;
        if (db == null) {
            db = dbHelper.getWritableDatabase();
        }
        entity.prewrite();
        if (entity._status == Entity.MANAGED) {
            success = db.delete(entity.getTableName(), "_id=?",
                    new String[] { String.valueOf(entity._id) }) > 0;
            entity._status = Entity.REMOVED;
        }
        entity.postwrite();
        return success;
    }

    public boolean remove(Entity entity, String whereClause, String[] whereArgs){
        boolean success = false;
        if (db == null) {
            db = dbHelper.getWritableDatabase();
        }
        entity.prewrite();
        if (entity._status == Entity.MANAGED) {
            success = db.delete(entity.getTableName(), whereClause, whereArgs) > 0;
            entity._status = Entity.REMOVED;
        }
        entity.postwrite();
        return success;
    }

    public void deleteAll(EntityManager entityManager, Class<? extends Entity> clazz) {
        if (entityManager != null) {
            try {
                boolean result = entityManager.execSQL("DELETE FROM " + clazz.getSimpleName());
            } catch (Exception e) {
            } finally {
                if (entityManager != null) {
                    entityManager.close();
                }
            }
        }
    }

    public boolean isOpen() {
        return true;
    }

    public void close() { }

    public EntityTransaction getTransaction() {
        if (transaction == null) {
            transaction = new EntityTransaction(dbHelper);
        }
        return transaction;
    }

    //================================================================================================private

    private boolean createTable(String tableName, Entity entity, boolean isForceCreate){
        boolean isCreated = false;
        if(!isForceCreate){	// 不是强制创建表，则先查询缓存中是否曾经创建过
            if(createTableCache.containsKey(tableName)){
                isCreated = createTableCache.get(tableName);
            }
        }
        if(!isCreated){
            //
            if (db == null){
                db = dbHelper.getWritableDatabase();
            }
            isCreated = db.execSQL(TableBuilder.createSQLStatement(entity));
            // 消息表创建索引
            String sql = TableBuilder.createIndexSQLStatement(entity);
            if (sql != null) {
                db.execSQL(sql);
            }
            createTableCache.put(tableName, isCreated);
            /** 放到缓存里面  */
            if(isCreated) {
                db.addToTableCache(tableName);
            }
        }
        return isCreated;
    }

    private List<? extends Entity> queryInner(Class<? extends Entity> clazz,
                                              String table, boolean distinct, String selection,
                                              String[] selectionArgs, String groupBy, String having,
                                              String orderBy, String limit,NoColumnErrorHandler handler) {

        List<? extends Entity> list = null;
        Cursor cursor = null;
        try {

            cursor = query(distinct, table, null, selection,
                    selectionArgs, groupBy, having, orderBy, limit);

            if (cursor != null) {
                cursor = new CursorOpt(cursor);
                list = cursor2List(clazz, table, cursor,handler);
            }
        } catch (Exception e) {

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return list;
    }

}
