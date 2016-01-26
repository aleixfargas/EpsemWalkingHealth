package epsem.walkinghealth.common;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class utils {
    public static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");


    /**
     * Method to write in log, always write in logs/log.log
     *
     * @param trace The file we are writting from
     * @param sequence The log sequence
     */
    public static final void log(String trace, String sequence) {
        Date date = new Date();

        File logFolder = new File(Environment.getExternalStorageDirectory(), "logs");
        if (!logFolder.exists()) {
            logFolder.mkdir();
        }

        File logFile = new File(logFolder, "log.log");

        try {
            FileWriter writer = new FileWriter(logFile, true);
            writer.write(formatter.format(date)+"--"+trace+":  "+sequence+"\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Method to write in log, always write in logs/
     *
     * @param trace The file we are writting from
     * @param sequence The log sequence
     * @param filename The log file name we want to write in
     */
    public static final void log(String trace, String sequence, String filename){
        Date date = new Date();

        File logFolder = new File(Environment.getExternalStorageDirectory(), "logs");
        if(!logFolder.exists()){
            logFolder.mkdir();
        }

        File logFile = new File(logFolder, filename+".log");

        try {
            FileWriter writer = new FileWriter(logFile,true);
            writer.write(formatter.format(date)+"--"+trace+":  "+sequence+"\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Method to get the dateTime formated as String Object
     *
     * @param date
     * @return
     *      String containing the dateTime of the object
     */
    public static final String getStringDateTime(Date date){
        return formatter.format(date);
    }


    /**
     * Method to get the date formated as String Object
     *
     * @return
     *      String containing the dateTime of the object
     */
    public static final String getStringDate(Date date) {
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");
        return formatDate.format(date);
    }


    /**
     * Method to get the current Hour in a String format of two digits
     *
     * @param date
     * @return
     */
    public static final String getStringCurrentHour(Date date){
        SimpleDateFormat formatHour = new SimpleDateFormat("HH");
        return formatHour.format(date);
    }

    public static final void copyFile(File src, File dst){
        try {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);

            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
        catch (IOException e){
            Log.e("utils","Copy error!");
        }
    }
}
