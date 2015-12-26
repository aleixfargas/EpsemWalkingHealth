package epsem.walkinghealth;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WriteFileManager {
    public GraphActivity graphActivity = null;
    public ArrayList<AccelData> results = new ArrayList<>();;

    private static final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();


    /**
     * Initializes a task that will be executed periodically.
     *
     * @param graphActivity
     *      Gets the GraphActivity to read the results received from the BluetoothGattCallback
     */
    public WriteFileManager(final GraphActivity graphActivity) {
        this.graphActivity = graphActivity;

        Runnable task = new Runnable() {
            public void run() {
                Log.e("WriteFileManager","auto-executing Writter task");
                results = concatenate(results, graphActivity.getResults());
                graphActivity.clearResults();
                if (results != null) {
                    Log.e("WriteFileManager", "results size = " + results.size());
                    try {
                        if(!writeFile()){
                            Log.e("WriteFileManager","Something wrong happened");
                        }
                    } catch (IOException e) {
                        Log.e("WriteFileManager","Failed to write in the File");
                    }
                }
                Log.e("WriteFileManager","end Writter task");
            }
        };
        worker.scheduleAtFixedRate(task, 30, 30, TimeUnit.SECONDS);
    }


    /**
     *  Concat two arrays
     *
     * @param target
     *      ArrayList<AccelData> type list that we want to fill with the src contents
     * @param src
     *      ArrayList<AccelData> type list that content the data we want to include in the target data

     * @return ArrayList<AccelData>
     *      Containing target+src data concatenated
     */
    public ArrayList<AccelData> concatenate(ArrayList<AccelData> target, ArrayList<AccelData> src) {
        if(!src.isEmpty()){
            Log.e("WriteFileManager", "concat target = "+target+" + src = "+src);
            for(AccelData r : src){
                target.add(r);
                Log.e("WriteFileManager", "concatenated " + r.toString());
            }
        }

        return target;
    }


    /**
     * Method to get the dateTime formated as String Object
     *
     * @return
     *      String containing the dateTime of the object
     */
    private String getStringDateTime() {
        // (1) get today's date
        Date today = Calendar.getInstance().getTime();

        // (2) create a date "formatter" (the date format we want)
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        // (3) create a new String using the date format we want
        return formatter.format(today);
    }


    /**
     * Method to check if exists a file, if not, create it.
     *
     * @param file
     *      File object to create
     *
     * @return Boolean
     *      True when success, false otherwise.
     * @throws IOException
     *      When failed
     */
    private Boolean createFile(File file) throws IOException {
        Boolean r = true;

        if (!file.exists()) {
            Log.e("WriteFileManager", "creating new File: " + file);
            if(!file.createNewFile()){
                r = false;
            }
        }

        return r;
    }


    /**
     * Method to check if exists a folder, if not, create it
     *
     * @param folder
     *      Folder we want to create
     * @return boolean
     *      True when success, false otherwise.
     */
    private Boolean createFolder(File folder){
        Boolean r = true;
        if (!folder.exists()) {
            Log.e("WriteFileManager", "creating new Folder: " + folder);
            if (!folder.mkdirs()) {
                r = false;
            }
        }

        return r;
    }


    /**
     * Method to write the results into the resultsFile.
     * It clears each result of the ArrayList<AccelData> results immediatly after writting it.
     * It has a maximum of 8190 writes for each call.
     *
     * @return Boolean
     *      True on success, otherwise false.
     *
     * @throws IOException
     *      When failed to create, open or write a file or directory
     */
    public Boolean writeFile() throws IOException {
        String now = getStringDateTime();
        String filename = now + "_data";
        String fileExtension = ".txt";

        File resultsFolder = new File(Environment.getExternalStorageDirectory(), "WalkingHealth");
        File resultsFile = new File(resultsFolder, filename+fileExtension);
        BufferedWriter output;
        FileWriter fw;

        Boolean success = false;
        int i = 0, wrote = 0, todelete = 0;

        if(createFolder(resultsFolder)) {
            if (createFile(resultsFile)) {
                //create a BufferedWritter to write in the file
                fw = new FileWriter(resultsFile);
                output = new BufferedWriter(fw);

                Log.e("WriteFileManager", "Writing " + this.results.size() + " results");
                for (i = 0; ((i < this.results.size()) /*|| (i < 8190)*/); i++) {
                    Log.e("WriteFileManager", "Wrote results[" + i + "] =" + this.results.get(i).toString());
                    if (this.results.get(i).toString() != null) {
                        output.write(this.results.get(i).toString());
                        wrote++;
                    }
                }

                Log.e("WriteFileManager", "flushing");
                output.flush();
                Log.e("WriteFileManager", "closing");
                output.close();
                Log.e("WriteFileManager", "start deleting");

                todelete = wrote;

                Log.e("WriteFileManager", "Wrote " + wrote + "results");
                while(todelete > 0){
                    Log.e("WriteFileManager", "Deleting results["+i+"] =" + this.results.get(i).toString());
                    this.results.remove(todelete);
                    todelete--;
                }

                Log.e("WriteFileManager", "Deleted " + (wrote - todelete) + "results");

                if(todelete == 0) {
                    success = true;
                }
            }
        }

        return success;
    }
}
