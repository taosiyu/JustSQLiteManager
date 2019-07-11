package com.sutdy2change.sqlitemanager;

import com.study2change.dbManager.ConflictClause;
import com.study2change.dbManager.Entity;
import com.study2change.dbManager.unique;
import com.study2change.dbManager.uniqueConstraints;

@uniqueConstraints(columnNames = "uin", clause = ConflictClause.REPLACE)
public class DemoEntity extends Entity {

    @unique
    public String uin;
    public int flags;
    public String name;
    public int age;
}
