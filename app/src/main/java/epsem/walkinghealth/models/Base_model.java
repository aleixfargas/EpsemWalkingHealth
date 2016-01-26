package epsem.walkinghealth.models;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import java.io.File;

import epsem.walkinghealth.common.config;
import epsem.walkinghealth.common.utils;

public class Base_model extends config {
    File db_copy = new File(Environment.getExternalStorageDirectory(), "database");
    File original_db = null;

    Base_model(Context context) {
        super(context);
        SQLiteDatabase db_read = super.getReadableDatabase();

        this.original_db = new File(db_read.getPath());
    }

    /**
     * Format:
     * SELECT columns FROM table
     * WHERE selection
     * AND selectionArgs
     * GROUP BY groupBy
     * ORDER BY orderBy
     *
     * @param table The table name to compile the query against.
     * @param columns A list of which columns to return. Passing null will return all columns, which is discouraged to prevent reading data from storage that isn't going to be used.
     * @param selection A filter declaring which rows to return, formatted as an SQL WHERE clause (excluding the WHERE itself). Passing null will return all rows for the given table.
     * @param selectionArgs You may include ?s in selection, which will be replaced by the values from selectionArgs, in order that they appear in the selection. The values will be bound as Strings.
     * @param groupBy A filter declaring how to group rows, formatted as an SQL GROUP BY clause (excluding the GROUP BY itself). Passing null will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in the cursor, if row grouping is being used, formatted as an SQL HAVING clause (excluding the HAVING itself). Passing null will cause all row groups to be included, and is required when row grouping is not being used.
     * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY itself). Passing null will use the default sort order, which may be unordered.
     * @return
     *      A Cursor object, which is positioned before the first entry.
     */
    public Cursor my_query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having,
                           String orderBy){
        SQLiteDatabase db_read = super.getReadableDatabase();

        Cursor res = db_read.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);

        utils.copyFile(this.original_db, this.db_copy);

        return res;
    }


    /**
     * INSERT INTO table(date, hour, name, done, uploaded) VALUES ('2016-01-04', '09', '2016-01-04_09_0.txt', 0,0);
     *
     * @param table the table to insert the row into
     * @param nullColumnHack optional; may be null. SQL doesn't allow inserting a completely empty row without naming at least one column name. If your provided values is empty, no column names are known and an empty row can't be inserted. If not set to null, the nullColumnHack parameter provides the name of nullable column name to explicitly insert a NULL into in the case where your values is empty.
     * @param values this map contains the initial column values for the row. The keys should be the column names and the values the column values
     * @return
     *      the row ID of the newly inserted row, or -1 if an error occurred
     */
    public long my_insert(String table, String nullColumnHack, ContentValues values) {
        long r = 0;
        SQLiteDatabase db_write = super.getWritableDatabase();
        r = db_write.insert(table, nullColumnHack, values);

        utils.copyFile(this.original_db, this.db_copy);

        return r;
    }


    /**
     * UPDATE values FROM table WHERE whereClause = whereArgs
     *
     * @param table the table to update in
     * @param values a map from column names to new column values. null is a valid value that will be translated to NULL.
     * @param whereClause the optional WHERE clause to apply when updating. Passing null will update all rows.
     * @param whereArgs You may include ?s in the where clause, which will be replaced by the values from whereArgs. The values will be bound as Strings.
     * @return
     *      the number of rows affected
     */
    public int my_update(String table, ContentValues values, String whereClause, String[] whereArgs){
        int r = 0;
        SQLiteDatabase db_write = super.getWritableDatabase();
        r = db_write.update(table, values, whereClause, whereArgs);

        utils.copyFile(this.original_db, this.db_copy);

        return r;
    }
}
