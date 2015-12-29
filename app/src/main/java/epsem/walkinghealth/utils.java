package epsem.walkinghealth;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class utils {
    public static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");

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

    public static final String getStringDateTime(Date date){
        return formatter.format(date);
    }
}
