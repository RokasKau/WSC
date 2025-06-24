package com.example.music2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "music_app.db";
    private static final int DB_VERSION = 3;

    private final Context context;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUserProgressTable = "CREATE TABLE IF NOT EXISTS user_progress (" +
                "category TEXT NOT NULL PRIMARY KEY, " +
                "level INTEGER NOT NULL, " +
                "experience INTEGER NOT NULL" +
                ");";

        String createWorkSessionTable = "CREATE TABLE IF NOT EXISTS work_session (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "work_minutes INTEGER NOT NULL, " +
                "break_minutes INTEGER NOT NULL, " +
                "session_count INTEGER NOT NULL" +
                ");";

        String createAlarmsTable = "CREATE TABLE IF NOT EXISTS alarms (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "hour INTEGER NOT NULL, " +
                "minute INTEGER NOT NULL, " +
                "is_active INTEGER NOT NULL CHECK(is_active IN (0,1)), " +
                "category TEXT NOT NULL, " +
                "created_at TEXT NOT NULL" +
                ");";

        db.execSQL(createUserProgressTable);
        db.execSQL(createWorkSessionTable);
        db.execSQL(createAlarmsTable);

        db.execSQL("INSERT OR IGNORE INTO user_progress (category, level, experience) VALUES ('chill', 1, 0);");
        db.execSQL("INSERT OR IGNORE INTO user_progress (category, level, experience) VALUES ('sleep', 1, 0);");
        db.execSQL("INSERT OR IGNORE INTO user_progress (category, level, experience) VALUES ('work', 1, 0);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("DatabaseHelper", "Upgrading DB from version " + oldVersion + " to " + newVersion);

        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS user_progress (" +
                    "category TEXT NOT NULL PRIMARY KEY, " +
                    "level INTEGER NOT NULL, " +
                    "experience INTEGER NOT NULL" +
                    ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS alarms (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "hour INTEGER NOT NULL, " +
                    "minute INTEGER NOT NULL, " +
                    "is_active INTEGER NOT NULL CHECK(is_active IN (0,1)), " +
                    "category TEXT NOT NULL, " +
                    "created_at TEXT NOT NULL" +
                    ");");

            db.execSQL("INSERT OR IGNORE INTO user_progress (category, level, experience) VALUES ('chill', 1, 0);");
            db.execSQL("INSERT OR IGNORE INTO user_progress (category, level, experience) VALUES ('sleep', 1, 0);");
            db.execSQL("INSERT OR IGNORE INTO user_progress (category, level, experience) VALUES ('work', 1, 0);");
        }

        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS work_session_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "work_minutes INTEGER NOT NULL, " +
                    "break_minutes INTEGER NOT NULL, " +
                    "session_count INTEGER NOT NULL" +
                    ");");

            try {
                db.execSQL("INSERT INTO work_session_new (id, work_minutes, break_minutes, session_count) " +
                        "SELECT id, work_hours * 60, break_minutes, session_count FROM work_session;");
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Failed migrating work_session table: " + e.getMessage());
            }

            db.execSQL("DROP TABLE IF EXISTS work_session;");
            db.execSQL("ALTER TABLE work_session_new RENAME TO work_session;");
        }
    }

    public int getUserLevel(String category) {
        SQLiteDatabase db = this.getReadableDatabase();
        int level = 1;
        Cursor cursor = db.rawQuery("SELECT level FROM user_progress WHERE category = ?", new String[]{category});
        if (cursor.moveToFirst()) {
            level = cursor.getInt(0);
        }
        cursor.close();
        return level;
    }

    public int getUserExperience(String category) {
        SQLiteDatabase db = this.getReadableDatabase();
        int exp = 0;
        Cursor cursor = db.rawQuery("SELECT experience FROM user_progress WHERE category = ?", new String[]{category});
        if (cursor.moveToFirst()) {
            exp = cursor.getInt(0);
        }
        cursor.close();
        return exp;
    }

    public void clearWorkSession() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("work_session", null, null);
        db.close();
    }

    public void updateUserProgress(String category, int level, int experience) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("level", level);
        values.put("experience", experience);

        int rowsUpdated = db.update("user_progress", values, "category = ?", new String[]{category});
        if (rowsUpdated == 0) {
            values.put("category", category);
            db.insert("user_progress", null, values);
        }
        db.close();
    }

    public WorkSession getLastWorkSession() {
        SQLiteDatabase db = this.getReadableDatabase();
        WorkSession session = null;
        Cursor cursor = db.rawQuery("SELECT id, work_minutes, break_minutes, session_count FROM work_session ORDER BY id DESC LIMIT 1", null);
        if (cursor.moveToFirst()) {
            session = new WorkSession(
                    cursor.getInt(0),
                    cursor.getInt(1),
                    cursor.getInt(2),
                    cursor.getInt(3)
            );
        }
        cursor.close();
        db.close();
        return session;
    }
    public void saveAlarm(int hour, int minute, boolean isActive, String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("hour", hour);
        values.put("minute", minute);
        values.put("is_active", isActive ? 1 : 0);
        values.put("category", category);
        values.put("created_at", System.currentTimeMillis());

        Cursor cursor = db.rawQuery("SELECT id FROM alarms WHERE hour = ? AND minute = ? AND category = ?",
                new String[]{String.valueOf(hour), String.valueOf(minute), category});

        if (cursor.moveToFirst()) {
            int id = cursor.getInt(0);
            db.update("alarms", values, "id = ?", new String[]{String.valueOf(id)});
        } else {
            db.insert("alarms", null, values);
        }
        cursor.close();
        db.close();
    }
}
