package epsem.walkinghealth.models;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.Date;

import epsem.walkinghealth.common.utils;

public class WriteFileManager_model extends Base_model {
    private String table = null;
    private String nullColumnHack = null;
    private ContentValues values = null;
    private String whereClause = null;
    private String[] whereArgs = null;
    private String[] columns = null;
    private String selection = null;
    private String[] selectionArgs = null;
    private String groupBy = null;
    private String having = null;
    private String orderBy = null;

    public WriteFileManager_model(Context context) {
        super(context);
    }


    /**
     * Return if the File 'name' exists
     *
     * @param name The name of the file we are finding
     * @return
     *      -1 when not founded
     *      id when founded
     */
    public int existFile(String name){
        int id = -1;
        this.table = "Files";
        this.columns = new String[]{
                "id"
        };
        this.selection = "name LIKE ?";
        this.selectionArgs = new String[]{
                name
        };
        this.groupBy = null;
        this.having = null;
        this.orderBy = null;

        Cursor cursor = this.my_query(this.table,this.columns, this.selection, this.selectionArgs, this.groupBy, this.having, this.orderBy);

        while(cursor.moveToNext()){
            id = cursor.getInt(cursor.getColumnIndex("id"));
        }
        cursor.close();
        utils.log("WFMmodel", "The file "+name+" has id = "+id);

        return id;
    }


    /**
     * Get the next Number of file that would be used to create a new file.
     *
     * @param date The date we will create the file
     * @param hour The hour we will create the file
     * @return
     *      An Integer representing the next file number
     */
    /*
    public Integer getFileNumber(Date date, String hour){
        int number = -1;
        this.table = "Files";
        this.columns = new String[]{
                "count(*)"
        };
        this.selection = "date = ? AND hour = ? AND done = 1";
        this.selectionArgs = new String[]{
                utils.getStringDateTime(date),
                hour
        };
        this.groupBy = null;
        this.having = null;
        this.orderBy = null;

        Cursor cursor = this.my_query(this.table,this.columns, this.selection, this.selectionArgs, this.groupBy, this.having, this.orderBy);

        cursor.moveToFirst();
        //number = cursor.getInt(cursor.getColumnIndex("counter"));
        number = cursor.getInt(0);
        cursor.close();

        utils.log("WFMmodel", "NextNumber is "+number);

        return number;
    }
*/

    /**
     * Insert into Files a new register file
     *
     * @param date The date when the file was created
     * @param hour The hour when the file was created represented by an Integer
     * @param name The name of the file
     */
    public void insert_newFile(Date date, String hour, String name) {
        this.table = "Files";
        this.nullColumnHack = "0";
        this.values = new ContentValues();
        this.values.put("date", utils.getStringDate(date));
        this.values.put("hour", hour);
        this.values.put("name", name);

        this.my_insert(this.table, this.nullColumnHack, this.values);
    }


    public int isDone(int id){
        int done = 0;

        this.table = "Files";
        this.columns = new String[]{
                "done"
        };
        this.selection = "id = "+id;
        this.selectionArgs = null;
        this.groupBy = null;
        this.having = null;
        this.orderBy = null;

        Cursor cursor = this.my_query(this.table,this.columns, this.selection, this.selectionArgs, this.groupBy, this.having, this.orderBy);
        if(cursor.moveToFirst()) {
            done = cursor.getInt(cursor.getColumnIndex("done"));
        }

        return done;
    }

    /**
     * Set a File to done
     *
     * @param id
     */
    public void done(int id) {
        int rows_affected = 0;
        this.table = "Files";
        this.values = new ContentValues();
        this.values.put("done", "1");
        this.whereClause = "id = "+id;
        this.whereArgs = null;

        rows_affected = this.my_update(this.table, this.values, this.whereClause, this.whereArgs);
        utils.log("WFMmodel","affected "+rows_affected+" rows");
    }
}