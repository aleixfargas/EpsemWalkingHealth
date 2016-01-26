package epsem.walkinghealth.common;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class config extends SQLiteOpenHelper{
    private static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "WalkingHealth";
    private static final String DICTIONARY_TABLE_NAME = "Files";
    private static final String DICTIONARY_TABLE_CREATE =
            "CREATE TABLE IF NOT EXISTS " + DICTIONARY_TABLE_NAME + " ("+
                "id INTEGER PRIMARY KEY, " +
                "date TEXT NOT NULL, " +
                "hour TEXT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "done INTEGER NOT NULL DEFAULT '0', " +
                "uploaded INTEGER NOT NULL DEFAULT '0'" +
            ");";


    protected config(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DICTIONARY_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}