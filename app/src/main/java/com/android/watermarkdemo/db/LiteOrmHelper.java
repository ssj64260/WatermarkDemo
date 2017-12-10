package com.android.watermarkdemo.db;

import android.content.Context;

import com.litesuits.orm.LiteOrm;
import com.litesuits.orm.db.assit.QueryBuilder;

import java.util.List;

/**
 * 数据库工具
 */

public class LiteOrmHelper {

    private final String DB_NAME = "WatermarkDemo.db";

    private final boolean DEBUGGABLE = true; // 是否输出log

    private LiteOrm liteOrm;

    public LiteOrmHelper(Context context) {
        liteOrm = LiteOrm.newSingleInstance(context, DB_NAME);
        liteOrm.setDebugged(DEBUGGABLE);
    }

    public <T> T queryFirst(Class<T> cla) {
        final List<T> list = liteOrm.query(cla);
        if (list != null && list.size() > 0) {
            return list.get(0);
        } else {
            return null;
        }
    }

    public <T> List<T> queryAll(Class<T> cla) {
        return liteOrm.query(cla);
    }

    public <T> List<T> queryAllOrderDescBy(Class<T> cla, String orderBy) {
        return liteOrm.query(new QueryBuilder<>(cla).appendOrderDescBy(orderBy));
    }

    public <T> long save(T objec) {
        return liteOrm.save(objec);
    }

    public <T> long saveAll(List<T> objects) {
        return liteOrm.save(objects);
    }

    public <T> int update(T object) {
        return liteOrm.update(object);
    }

    public <T> int delete(T object) {
        return liteOrm.delete(object);
    }

    public <T> int deleteAll(Class<T> cla) {
        return liteOrm.delete(cla);
    }

    public void closeDB() {
        if (liteOrm != null) {
            liteOrm.close();
        }
    }
}
