package com.chteuchteu.munin.db;

import android.content.ContentValues;
import android.database.Cursor;

import com.chteuchteu.munin.obj.Entity;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseRepository<T> {
    protected static String COLUMN_ID = "id";

    protected Database database;

    public BaseRepository(Database database) {
        this.database = database;
        this.items = new ArrayList<>();
    }

    public List<T> findAll() {
        return findAll(null);
    }

    public List<T> findAll(String orderBy) {
        List<T> list = new ArrayList<>();

        Cursor cursor = cursorFindAll(orderBy);
        while (!cursor.isAfterLast()) {
            T item = cursorToT(cursor);
            list.add(item);

            if (!items.contains(item))
                items.add(item);

            cursor.moveToNext();
        }

        cursor.close();
        return list;
    }

    public List<T> findByParent(Entity parent) {
        List<T> list = new ArrayList<>();

        Cursor cursor = cursorFindAll(null); // TODO FIND BY
        while (!cursor.isAfterLast()) {
            list.add(cursorToT(cursor, parent));
            cursor.moveToNext();
        }

        cursor.close();
        return list;
    }

    public synchronized Cursor cursorFindAll(String orderBy) {
        Cursor cursor = database.getWritableDatabase().query(getTable(), getAllColumns(), null, null, null, null, orderBy);
        cursor.moveToFirst();
        return cursor;
    }

    public T findById(Long id) {
        return null;
    }

    public abstract long save(T t);

    public long save(Long id, String[] keys, Object[] values) {
        if (keys.length != values.length)
            throw new IllegalArgumentException("Keys size != values size");


        ContentValues contentValues = new ContentValues();
        for (int i=0; i<keys.length; i++)
            addToContentValues(contentValues, keys[i], values[i]);

        if (id == -1)
            return database.getWritableDatabase().insertOrThrow(getTable(), null, contentValues);
        else {
            if (findById(id) != null)
                database.getWritableDatabase().update(getTable(), contentValues, COLUMN_ID + " = ?", new String[] { id.toString() });
            else {
                contentValues.put(COLUMN_ID, id);
                database.getWritableDatabase().insertOrThrow(getTable(), null, contentValues);
            }
            return id;
        }
    }

    private void addToContentValues(ContentValues contentValues, String key, Object value) {
        if (value == null)
            contentValues.put(key, (String) null);
        else if (value instanceof String)
            contentValues.put(key, (String) value);
        else if (value instanceof Long)
            contentValues.put(key, (Long) value);
        else if (value instanceof Double)
            contentValues.put(key, (Double) value);
        else if (value instanceof Integer)
            contentValues.put(key, (Integer) value);
        else if (value instanceof Boolean)
            contentValues.put(key, ((Boolean) value ? 1 : 0));
        else if (value instanceof Float)
            contentValues.put(key, ((Float) value).doubleValue());
        else if (value instanceof byte[])
            contentValues.put(key, (byte[]) value);
        else
            throw new IllegalArgumentException("Value of " + key + "=" + value + " type is not supported.");
    }

    protected String getString(Cursor cursor, String columnName) {
        return cursor.getString(cursor.getColumnIndex(columnName));
    }

    public Double getDouble(Cursor cursor, String columnName) {
        return cursor.getDouble(cursor.getColumnIndex(columnName));
    }

    public Float getFloat(Cursor cursor, String columnName) {
        return cursor.getFloat(cursor.getColumnIndex(columnName));
    }

    public Integer getInteger(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        if (cursor.isNull(columnIndex))
            return null;
        else
            return cursor.getInt(columnIndex);
    }

    public Boolean getBoolean(Cursor cursor, String columnName) {
        Integer value = getInteger(cursor, columnName);
        if (value == null)
            return null;
        else
            return value == 1;
    }

    public Long getLong(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        if (cursor.isNull(columnIndex))
            return null;
        else
            return cursor.getLong(columnIndex);
    }

    protected abstract String getTable();
    protected abstract String[] getAllColumns();

    protected T cursorToT(Cursor cursor) {
        return cursorToT(cursor, null);
    }
    protected abstract T cursorToT(Cursor cursor, Entity parent);
}
