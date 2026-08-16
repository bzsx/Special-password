package com.bzsx.password;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "password_manager.db";
    private static final int DATABASE_VERSION = 4;

    private static final String COLUMN_WEBSITE = "website";
    private static final String TABLE_PASSWORDS = "passwords";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_ACCOUNT = "account";
    private static final String COLUMN_ENCRYPTED_PASSWORD = "encrypted_password";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";
    private static final String COLUMN_SORT_ORDER = "sort_order";

    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_PASSWORDS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT NOT NULL, " +
                    COLUMN_ACCOUNT + " TEXT NOT NULL, " +
                    COLUMN_ENCRYPTED_PASSWORD + " TEXT NOT NULL, " +
                    COLUMN_CREATED_AT + " INTEGER NOT NULL, " +
                    COLUMN_UPDATED_AT + " INTEGER NOT NULL, " +
                    COLUMN_SORT_ORDER + " INTEGER NOT NULL DEFAULT 0" +
                    ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
        // 新数据库也补上 website 列（与升级后的表结构保持一致）
        db.execSQL("ALTER TABLE " + TABLE_PASSWORDS + " ADD COLUMN " + COLUMN_WEBSITE + " TEXT DEFAULT ''");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 安全升级，避免字段已存在导致 ALTER 崩溃，进而使数据库损坏、写入静默失败
        if (oldVersion < 2 && !columnExists(db, COLUMN_SORT_ORDER)) {
            db.execSQL("ALTER TABLE " + TABLE_PASSWORDS + " ADD COLUMN " + COLUMN_SORT_ORDER + " INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 3 && !columnExists(db, COLUMN_WEBSITE)) {
            db.execSQL("ALTER TABLE " + TABLE_PASSWORDS + " ADD COLUMN " + COLUMN_WEBSITE + " TEXT DEFAULT ''");
        }
        // 版本4：二次自愈，确保两个列都存在（防止历史库损坏后字段缺失）
        if (oldVersion < 4) {
            if (!columnExists(db, COLUMN_SORT_ORDER)) {
                db.execSQL("ALTER TABLE " + TABLE_PASSWORDS + " ADD COLUMN " + COLUMN_SORT_ORDER + " INTEGER NOT NULL DEFAULT 0");
            }
            if (!columnExists(db, COLUMN_WEBSITE)) {
                db.execSQL("ALTER TABLE " + TABLE_PASSWORDS + " ADD COLUMN " + COLUMN_WEBSITE + " TEXT DEFAULT ''");
            }
        }
    }

    /**
     * 检查某列是否已存在（避免 ALTER 重复添加报错）
     */
    private boolean columnExists(SQLiteDatabase db, String columnName) {
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + TABLE_PASSWORDS + ")", null);
        try {
            int nameIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                String name = cursor.getString(nameIndex);
                if (columnName.equals(name)) {
                    return true;
                }
            }
        } finally {
            cursor.close();
        }
        return false;
    }

    public long insertPassword(PasswordEntry entry) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, entry.getName());
        values.put(COLUMN_ACCOUNT, entry.getAccount());
        values.put(COLUMN_ENCRYPTED_PASSWORD, entry.getEncryptedPassword());
        values.put(COLUMN_CREATED_AT, entry.getCreatedAt());
        values.put(COLUMN_UPDATED_AT, entry.getUpdatedAt());
        values.put(COLUMN_SORT_ORDER, entry.getSortOrder());
        values.put(COLUMN_WEBSITE, entry.getWebsite() != null ? entry.getWebsite() : "");
        long id = db.insert(TABLE_PASSWORDS, null, values);
        db.close();
        return id;
    }

    public int updatePassword(PasswordEntry entry) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, entry.getName());
        values.put(COLUMN_ACCOUNT, entry.getAccount());
        values.put(COLUMN_ENCRYPTED_PASSWORD, entry.getEncryptedPassword());
        values.put(COLUMN_UPDATED_AT, System.currentTimeMillis());
        values.put(COLUMN_WEBSITE, entry.getWebsite() != null ? entry.getWebsite() : "");
        int rows = db.update(TABLE_PASSWORDS, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(entry.getId())});
        db.close();
        return rows;
    }

    public int deletePassword(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_PASSWORDS, COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    public PasswordEntry getPassword(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PASSWORDS, null, COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null);
        PasswordEntry entry = null;
        if (cursor != null && cursor.moveToFirst()) {
            entry = cursorToEntry(cursor);
            cursor.close();
        }
        db.close();
        return entry;
    }

    public List<PasswordEntry> getAllPasswords() {
        List<PasswordEntry> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PASSWORDS, null, null, null,
                null, null, COLUMN_SORT_ORDER + " DESC, " + COLUMN_UPDATED_AT + " DESC");
        while (cursor != null && cursor.moveToNext()) {
            list.add(cursorToEntry(cursor));
        }
        if (cursor != null) cursor.close();
        db.close();
        return list;
    }

    public boolean updateSortOrder(long id, long sortOrder) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SORT_ORDER, sortOrder);
        values.put(COLUMN_UPDATED_AT, System.currentTimeMillis());
        int rows = db.update(TABLE_PASSWORDS, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
        return rows > 0;
    }

    private PasswordEntry cursorToEntry(Cursor cursor) {
        PasswordEntry entry = new PasswordEntry();
        entry.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
        entry.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
        entry.setAccount(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT)));
        entry.setEncryptedPassword(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENCRYPTED_PASSWORD)));
        entry.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
        entry.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)));
        entry.setSortOrder(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SORT_ORDER)));
        // website 字段可能老版本不存在，做兼容
        try {
            int websiteIdx = cursor.getColumnIndexOrThrow(COLUMN_WEBSITE);
            entry.setWebsite(cursor.getString(websiteIdx));
        } catch (Exception ignored) {
            entry.setWebsite("");
        }
        return entry;
    }
}