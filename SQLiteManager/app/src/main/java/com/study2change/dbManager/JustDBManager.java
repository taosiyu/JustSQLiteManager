package com.study2change.dbManager;

import android.content.Context;

import java.util.List;

public class JustDBManager {

    private static String dbName = "demotest";

    private SQLiteManagerCenter mCenter;

    private EntityManager mManager;//mManager功能丰富，where,orderBy等

    private static volatile JustDBManager instance=null;

    private Object dbLock = new Object();

    private JustDBManager(Context context) {
        mCenter = new SQLiteManagerCenter(dbName, context);
        mManager = mCenter.createEntityManager();
    }

    public static JustDBManager getInstance(Context context){
        if(instance==null){
            synchronized(JustDBManager .class){
                if(instance==null){
                    instance=new JustDBManager(context);
                }
            }
        }
        return instance;
    }

    //================================================public

    public void insertEntity(List<Entity> list){
        if (list == null){
            return;
        }

        if (list.size() <= 0){
            return;
        }

        synchronized (dbLock){
            EntityTransaction trans = mManager.getTransaction();
            try
            {
                trans.begin();
                int listSize = list.size();
                for (int i = 0; i < listSize; ++i)
                {
                    Entity entity = list.get(i);
                    mManager.persistOrReplace(entity);
                }
                trans.commit();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                trans.end();
            }
            mManager.close();
        }
    }

    public void removeEntity(List<Entity> list) {
        if (list == null){
            return;
        }

        if (list.size() <= 0){
            return;
        }

        synchronized (dbLock){
            EntityTransaction trans = mManager.getTransaction();
            try
            {
                trans.begin();
                int listSize = list.size();
                for (int i = 0; i < listSize; ++i)
                {
                    Entity entity = list.get(i);
                    mManager.remove(entity);
                }
                trans.commit();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                trans.end();
            }
            mManager.close();
        }
    }

    public List<Entity> getAllEntity(Entity entity){
        List<Entity> list = null;
        synchronized(dbLock){
            try{
                list = (List<Entity>) mManager.query(entity.getClass());
            }catch (Exception e) {
            }
            finally
            {
                mManager.close();
            }
        }

        return list;
    }


}
