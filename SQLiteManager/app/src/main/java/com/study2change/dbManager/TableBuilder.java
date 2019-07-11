package com.study2change.dbManager;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TableBuilder {

    public static final String PRIMARY_KEY = "_id";
    public static final Map<Class<?>, String> TYPES = new HashMap<Class<?>, String>();
    private static final Map<Class<? extends Entity>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<Class<? extends Entity>, List<Field>>();
    private static final Map<Class<? extends Entity>, List<Field>> ALL_FIELD_CACHE = new ConcurrentHashMap<Class<? extends Entity>, List<Field>>();

    // 缓存创建表的SQL语句
    private static final Map<String, String> CREATE_TABLE_CACHE = new ConcurrentHashMap<String, String>();

    private static final Map<Class<? extends Entity>, Entity> TABLE_CACHE = new ConcurrentHashMap<Class<? extends Entity>, Entity>();

    static {
        TYPES.put(byte.class, "INTEGER");
        TYPES.put(boolean.class, "INTEGER");
        TYPES.put(short.class, "INTEGER");
        TYPES.put(int.class, "INTEGER");
        TYPES.put(long.class, "INTEGER");
        TYPES.put(String.class, "TEXT");
        TYPES.put(byte[].class, "BLOB");
        TYPES.put(float.class, "REAL");
        TYPES.put(double.class, "REAL");
    }

    public static Entity getTableConfig(Class<? extends Entity> clazz) throws InstantiationException, IllegalAccessException {
        Entity entity = TABLE_CACHE.get(clazz);
        if (null == entity) {
            entity = clazz.newInstance();
            TABLE_CACHE.put(clazz, entity);
        }
        return entity;
    }

    public static String getTableName(Class<? extends Entity> clazz) throws InstantiationException, IllegalAccessException {
        Entity entity = getTableConfig(clazz);
        return entity.getTableName();
    }

    public static String getTableNameSafe(Class<? extends Entity> clazz) {
        try {
            return getTableName(clazz);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String createSQLStatement(Entity entity) {
        String tableName = entity.getTableName();

        // 用缓存创建表的SQL语句，首次登录能加快2~4秒
        if (CREATE_TABLE_CACHE.containsKey(tableName)) {
            return CREATE_TABLE_CACHE.get(tableName);
        }

        StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sb.append(tableName);
        sb.append(" (" + PRIMARY_KEY + " INTEGER PRIMARY KEY AUTOINCREMENT");
        Class<? extends Entity> clz = entity.getClassForTable();
        List<Field> f = getValidField(clz);

        Field field = null;
        for (int i = 0, fLen = f.size(); i < fLen; i ++) {
            field = f.get(i);

            String name = field.getName();
            Class c = field.getType();
            String type = TYPES.get(c);
            if (type != null) {
                sb.append(',');
                sb.append(name + " " + type);
                if (field.isAnnotationPresent(unique.class)) {
                    sb.append(" UNIQUE");
                }else if(field.isAnnotationPresent(defaultzero.class)){
                    sb.append(" default " + 0);
                } else if(field.isAnnotationPresent(defaultValue.class)) {
                    sb.append(" default " + field.getAnnotation(defaultValue.class).defaultInteger());
                }
            }
        }
        if (clz.isAnnotationPresent(uniqueConstraints.class)) {
            uniqueConstraints constraints = (uniqueConstraints) clz.getAnnotation(uniqueConstraints.class);
            String columnName = constraints.columnNames();
            sb.append(",UNIQUE(" + columnName + ")");
            String clause = constraints.clause().toString();
            sb.append(" ON CONFLICT " + clause);
        }
        sb.append(')');
        String sqlValue = sb.toString();

        CREATE_TABLE_CACHE.put(tableName, sqlValue);
        return sqlValue;

    }

    public static String createIndexSQLStatement(Entity entity) {
            String tableName = entity.getTableName();

            StringBuilder sb = new StringBuilder("CREATE INDEX IF NOT EXISTS ");
            sb.append(tableName).append("_idx");
            sb.append(" ON ");
            sb.append(tableName);
            String column = "time";
            sb.append("(").append(column).append(", _id)");
            return sb.toString();
    }

    public static List<Field> getValidField(Class<? extends Entity> clazz) {
        Class<? extends Entity> classForTable = null;

        try {
            Entity instance = TableBuilder.getTableConfig(clazz);
            classForTable = instance.getClassForTable();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        List<Field> fields = FIELD_CACHE.get(classForTable);
        if (fields == null) {
            Field[] f = classForTable.getFields();
            fields = new ArrayList<Field>(f.length);
            for (Field field : f) {
                // 跳过notColumn修饰的字段
                if (!Modifier.isStatic(field.getModifiers()) && !field.isAnnotationPresent(notColumn.class)) {
                    fields.add(field);
                }
            }
            FIELD_CACHE.put(classForTable, fields);
        }
        return fields;
    }

    public static List<Field> getValidField(Entity entity) {

        Class<? extends Entity> classForTable = entity.getClassForTable();

        List<Field> fields = FIELD_CACHE.get(classForTable);
        if (fields == null) {
            Field[] f = classForTable.getFields();
            fields = new ArrayList<Field>(f.length);
            for (Field field : f) {
                // 跳过notColumn修饰的字段
                if (!Modifier.isStatic(field.getModifiers()) && !field.isAnnotationPresent(notColumn.class)) {
                    fields.add(field);
                }
            }
            FIELD_CACHE.put(classForTable, fields);
        }
        return fields;
    }

    // 不删表更新数据库的方法，添加字段，并且赋初值0
    public static String addColumn(String tableName, String columnName, String clumnType, boolean needSetDefault) {
        return addColumn(tableName, columnName, clumnType, needSetDefault, 0);
    }

    public static String addColumn(String tableName, String columnName, String clumnType, boolean needSetDefault, int defaultInt) {
        String sql;
        if(needSetDefault){
            sql = "alter table " + tableName + " add " + columnName + " " + clumnType + " default " + defaultInt;
        }else{
            sql = "alter table " + tableName + " add " + columnName + " " + clumnType;
        }
        return sql;
    }

    public static String dropSQLStatement(String tableName) {
        return "DROP TABLE IF EXISTS " + tableName;
    }

}
