package epsem.walkinghealth;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WriteFileManager {
    private static final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    public GraphActivity graphActivity = null;
    public ArrayList<AccelData> results = new ArrayList<>();;

    /**
     * Initializes a task that will be executed after every minute.
     *
     * @param graphActivity
     *      Gets the GraphActivity to read the results received from the BluetoothGattCallback
     */
    public WriteFileManager(final GraphActivity graphActivity) {
        this.graphActivity = graphActivity;
        Log.e("WriteFile","creating Writter task");

        Runnable task = new Runnable() {
            public void run() {
                Log.e("WriteFile","auto-executing Writter task");
                results = concatenate(results, graphActivity.getResults());
                Log.e("WriteFile", "Clearing Results");
                graphActivity.clearResults();
                Log.e("WriteFile", "Cleared Results");
                if (results != null) {
                    Log.e("WriteFile", "results size = " + results.size());
                    writeFile();
                }
            }
        };
        worker.scheduleAtFixedRate(task, 30, 30, TimeUnit.SECONDS);
    }


    private String getStringDateTime() {
        // (1) get today's date
        Date today = Calendar.getInstance().getTime();

        // (2) create a date "formatter" (the date format we want)
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        // (3) create a new String using the date format we want
        return formatter.format(today);
    }


    private BufferedWriter NewFileCreation(File file) {
        BufferedWriter newFile_bw = null;
        try {
            if (file.createNewFile()) {
                Log.e("new File", "Success Creating");
                FileWriter fw = new FileWriter(file);
                newFile_bw = new BufferedWriter(fw);
            } else {
                Log.e("new File", "error Creating");
            }
        } catch (Exception IOException) {
            Log.e("new writter error", "Cannot create new file");
        }

        return newFile_bw;
    }


    private BufferedWriter checkFolderFiles(File newFolder, String filename) {
        BufferedWriter file_output = null;

        File[] listOfFiles = newFolder.listFiles();
        for (File f : listOfFiles) {
            Log.e("Checking Folder", "founded file: " + f);
            if (f.isFile()) {
                Log.e("Checking Folder", f + " is file and his name is: " + f.getName());
                if (f.getName().equals(filename)) {
                    Log.e("Checking Folder", f.getName() + " == " + filename);
                    try {
                        file_output = new BufferedWriter(new FileWriter(f, true));
                    }
                    catch (Exception IOException) {
                        Log.e("old file error", "Cannot open oldFile");
                    }
                }
            }
        }

        return file_output;
    }


    public void writeFile() {
        int i = 0;
        BufferedWriter output;

        //return the current date String formatted
        String now = getStringDateTime();
        //Define here the name of the file
        String filename = now + "_data.txt";

        File newFolder = new File(Environment.getExternalStorageDirectory(), "WalkingHealth");
        Log.e("Folder", "new Folder: " + newFolder);

        if (!newFolder.exists()) {
            Log.e("Folder", "creating...");
            if (newFolder.mkdirs()) {
                Log.e("Folder", "Success Creating");
            }
            else{
                Log.e("Folder", "error Creating");
            }
        }

        //checking Folder in order to find if we have the same datetime file if founded, create a new FileWritter
        output = checkFolderFiles(newFolder, filename);

        if (output == null) {
            //file not exists, so we create it and create a new FileWritter
            File file = new File(newFolder, filename);
            output = NewFileCreation(file);
        }

        try {
            for (i = 0; i < this.results.size(); i++) {
                Log.e("Writting", "results["+i+"] ="+ this.results.get(i).toString());
                output.write(this.results.get(i).toString());
                results.remove(i);
            }
            if (output != null) {
                output.flush();
                output.close();
            }
        }
        catch (Exception IOException) {
            Log.e("Write in file", "Exception: " + IOException.getMessage());
        }
    }


    public ArrayList<AccelData> concatenate(ArrayList<AccelData> a, ArrayList<AccelData> b) {
        int aLen = a.size();
        int bLen = b.size();
        ArrayList<AccelData> c = new ArrayList<>(aLen+bLen);

        if(!a.isEmpty() && b.isEmpty()){
            Log.e("concatenating", "c =  b");
            for(AccelData r : a){
                c.add(r);
            }
            a.clear();
        }
        else if(a.isEmpty() && !b.isEmpty()){
            Log.e("concatenate", "concatenating c =  a");
            for(AccelData r : b){
                c.add(r);
            }
            b.clear();
        }
        else if(!a.isEmpty() && !b.isEmpty()){
            Log.e("concatenate", "concatenating a = " + aLen + "+ b =" + bLen);

            for(AccelData r1 : a){
                c.add(r1);
            }
            for(AccelData r2 : b){
                c.add(r2);
            }
            a.clear();
            b.clear();

            Log.e("concatenate", "concatenated");
        }
        else{
            Log.e("concatenate", "concatenated c = null");
        }

        return c;
    }
}
