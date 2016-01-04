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
    public static final long MAX_LENGTH = 8000000;


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
                utils.log("WriteFileManager", "auto-executing Writter task");
                results = concat(results, graphActivity.getResults());

                if (results != null) {
                    utils.log("WriteFileManager", "results size = " + results.size());
                    try {
                        if(!writeFile()){
                            Log.e("WriteFileManager", "Something wrong happened");
                        }
                    } catch (IOException e) {
                        Log.e("WriteFileManager","Failed to write in the File");
                    }
                }
                utils.log("WriteFileManager", "end Writter task");
            }
        };
        worker.scheduleAtFixedRate(task, 30, 30, TimeUnit.SECONDS);
    }


    /**
     *  Concat two arrays, deleting the src Array
     *
     * @param target
     *      ArrayList<AccelData> type list that we want to fill with the src contents
     * @param src
     *      ArrayList<AccelData> type list that content the data we want to include in the target data

     * @return ArrayList<AccelData>
     *      Containing target+src data concatenated
     */
    public ArrayList<AccelData> concat(ArrayList<AccelData> target, ArrayList<AccelData> src) {
        graphActivity.clearResults();

        if(!src.isEmpty()){
            for(AccelData r : src){
                target.add(r);
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

        // (2) create a date "formatter"
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        // (3) create a new String using the date format we want
        return formatter.format(today);
    }


    /**
     * Method to get the current Hour in a String format of two digits
     *
     * @return
     */
    private String getStringCurrentHour() {
        // (1) get today's date
        Date date = new Date();

        // (2) create a date "formatter"
        SimpleDateFormat formatter = new SimpleDateFormat("HH");
        utils.log("WriteFileManager", "Current hour = " + formatter.format(date));
        // (3) create a new String using the date format we want
        return formatter.format(date);
    }


    /**
     * Method to get the file Number
     *
     * @param line
     * @param folder
     * @return int
     *      Number identifier of the current results file
     */
    private int getFileNumber(String line, File folder) {
        int n = -1;

        File existentFile = null;
        String nextFileName = line+n;
        if(folder.exists()) {
            for (File file : folder.listFiles()) {
                if (file.isFile()) {
                    if ((file.getName()).contains(nextFileName)) {
                        nextFileName = line + n;
                        existentFile = file;
                        n++;
                    }
                }
            }
        }

        if(isFull(existentFile)){
            n++;
        }

        if(n == -1){
            n=0;
        }

        return n;
    }

    /**
     * Method to know if the file could not be bigger because, if it does, it could not be send to the server
     *
     * @param file
     *      File to know it size
     * @return Boolean
     */
    private Boolean isFull(File file){
        Boolean r = false;
        if(file != null) {
            r = ((file.length() > MAX_LENGTH) || (file.length() == MAX_LENGTH));
        }

        if(r){
            utils.log("WriteFileManager", "isFull!");
        }

        return r;
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
            utils.log("WriteFileManager", "creating new File: " + file);
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
            utils.log("WriteFileManager", "creating new Folder: " + folder);
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
     * If file reaches the MAX_LENGTH value, a new file will be created.
     * FileName format: 'ddMMAA_hh_X.txt'. 'X' value is the number of the file depending on the order it has been created.
     *
     * @return Boolean
     *      True on success, otherwise false.
     *
     * @throws IOException
     *      When failed to create, open or write a file or directory
     */
    public Boolean writeFile() throws IOException {
        File resultsFolder = new File(Environment.getExternalStorageDirectory(), "WalkingHealth");

        //Implementació del punt 2 del document: 'FunctionalDesign_WriteFileManager'
        String fileline = getStringDateTime()+"_"+getStringCurrentHour()+"_";
        int filenum = getFileNumber(fileline, resultsFolder);
        String fileExtension = ".txt";

        File resultsFile = new File(resultsFolder, fileline + filenum + fileExtension);

        BufferedWriter output;
        FileWriter fw;

        int i = 0;
        int wrote = 0;
        int todelete = 0;
        Boolean success = false;

        if(createFolder(resultsFolder)) {
            if (createFile(resultsFile)) {
                //create a BufferedWritter to write in the file
                fw = new FileWriter(resultsFile, true);
                output = new BufferedWriter(fw);

                utils.log("WriteFileManager", "Writing " + this.results.size() + " results");
                for (i = 0; ((i < this.results.size()) /*|| (i < 8190)*/); i++) {
                    if (this.results.get(i).toString() != null) {
                        output.write(this.results.get(i).toString());
                        wrote++;

                        if(isFull(resultsFile)) {
                            output.flush();
                            output.close();

                            filenum++;
                            resultsFile = new File(resultsFolder, fileline + filenum + fileExtension);
                            createFile(resultsFile);
                            fw = new FileWriter(resultsFile);
                            output = new BufferedWriter(fw);
                        }
                    }
                }

                utils.log("WriteFileManager", "Wrote " + wrote + " results");

                output.flush();
                output.close();

                todelete = wrote;

                utils.log("WriteFileManager", "Deleting " + todelete + " results");
                while(todelete > 0){
                    todelete--;
                    this.results.remove(todelete);
                }

                utils.log("WriteFileManager", "Deleted " + (wrote - todelete) + "results");

                if(todelete == 0) {
                    success = true;
                }
            }
        }

        return success;
    }
}

