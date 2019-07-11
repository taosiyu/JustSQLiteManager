package com.study2change.dbManager;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//存储本地所有的表名的cache
public class TableNameCache {
    public boolean isInit = false;

    private ConcurrentHashMap<String, Boolean> tbnCache = new ConcurrentHashMap<String, Boolean>();

    public void initTableCache(String[] tbs) {
        if(tbs == null) {
            return;
        }

        for(String name : tbs) {
            tbnCache.put(name, true);
        }
        isInit = true;
    }

    public String[] getAllTableNames() {
        Set<String> keyset = tbnCache.keySet();
        String[] tabNames = new String[keyset.size()];
        keyset.toArray(tabNames);

        return tabNames;
    }

    public void addToTableNameCache(String tableName) {
        tbnCache.put(tableName, true);
    }

    public void deleteFromTableCache(String tableName) {
        tbnCache.remove(tableName);
    }

    public boolean isContainsTableInCache(String tableName) {
        return tbnCache.containsKey(tableName);
    }


}
