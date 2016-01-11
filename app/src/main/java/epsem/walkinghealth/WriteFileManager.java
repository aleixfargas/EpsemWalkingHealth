package epsem.walkinghealth;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import epsem.walkinghealth.common.utils;
import epsem.walkinghealth.models.WriteFileManager_model;

public class WriteFileManager {
    public GraphActivity graphActivity = null;
    public ArrayList<AccelData> results = new ArrayList<>();;
    public String oldHour = "";
    public String oldDate = "";
    WriteFileManager_model WFMmodel = null;

    private static final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();

    static final long MAX_LENGTH = 8000000;
    static final String FILE_EXTENSION = ".txt";


    /**
     * Initializes a task that will be executed periodically.
     *
     * @param graphActivity
     *      Gets the GraphActivity to read the results received from the BluetoothGattCallback
     */
    public WriteFileManager(final GraphActivity graphActivity) {
        create_ma(graphActivity);

        Runnable task = new Runnable() {
            public void run() {
                utils.log("WriteFileManager", "auto-executing Writter task");
                results = concat(results, graphActivity.getResults());

                if (results != null) {
                    utils.log("WriteFileManager", "results size = " + results.size());
                    try {
                        utils.log("WriteFileManager","starting writeFile()");
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


    private void create_ma(GraphActivity graphActivity){
        this.graphActivity = graphActivity;
        utils.log("WriteFileManager","creating model");
        this.WFMmodel = new WriteFileManager_model(this.graphActivity);
        utils.log("WriteFileManager", "created model");
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
     * Method to get the file Number
     *
     * @param date
     * @param hourString
     * @return
     *      Number identifier of the current results file
     */
    private int getFileNumber(String date, String hourString) {
        int n = 0;
        int id = 0;
        id = WFMmodel.existFile(date+"_"+hourString+"_"+n+".txt");
        while (WFMmodel.isDone(id) == 1) {
            id = WFMmodel.existFile(date+"_"+hourString+"_"+(++n)+".txt");
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
     * Set the previous File as done
     */
    private void setOldFileDone(){
        int old_id = this.WFMmodel.existFile(this.oldDate+this.oldHour+getFileNumber(this.oldDate,this.oldHour)+".txt");
        if(old_id != -1){
            WFMmodel.done(old_id);
        }
    }


    /**
     * Check the time, the hour, and the filenumber and build a name for the next file.
     * If the previous file is not full but the time has changed, then mark the previous file to done.
     *
     * @return
     *      A String containing the filename
     */
    private String createFileName(Date now) {
        String fileDate = utils.getStringDate(now);
        if(!fileDate.equals(this.oldDate)){
            setOldFileDone();
            this.oldDate = fileDate;
        }

        String fileHour = utils.getStringCurrentHour(now);
        if(!fileHour.equals(this.oldHour)){
            setOldFileDone();
            this.oldHour = fileHour;
        }

        int filenum = getFileNumber(fileDate, fileHour);
        String filename = fileDate+"_"+fileHour+"_"+filenum+FILE_EXTENSION;

        return filename;
    }


    private int writeNow(File resultsFile) throws IOException{
        int wrote = 0;
        int i = 0;

        BufferedWriter output;
        FileWriter fw;

        //create a BufferedWritter to write in the file
        fw = new FileWriter(resultsFile, true);
        output = new BufferedWriter(fw);

        for (i = 0; ((i < this.results.size()) /*|| (i < 8190)*/); i++) {
            if (this.results.get(i).toString() != null) {
                output.write(this.results.get(i).toString());
                wrote++;

                if(isFull(resultsFile)) {
                    output.flush();
                    output.close();
                    break;
                }
            }
        }

        output.flush();
        output.close();

        return wrote;
    }


    private int deleteNow(int todelete){
        int deleted = 0;

        while(todelete > 0){
            todelete--;
            this.results.remove(todelete);
            deleted++;
        }

        return deleted;
    }


    public int writeTask(File resultsFile) throws IOException{
        int status = 0, towrite = 0, wrote = 0, todelete = 0, deleted = 0;

        towrite = this.results.size();
        utils.log("WriteFileManager", "Writing " + towrite + " results");
        wrote = writeNow(resultsFile);
        utils.log("WriteFileManager", "Wrote " + wrote + " results");

        todelete = wrote;

        utils.log("WriteFileManager", "Deleting " + todelete + " results");
        deleted = deleteNow(todelete);
        utils.log("WriteFileManager", "Deleted " + deleted + "results");

        if((todelete-deleted) == 0){
            //success
            status = 1;
            if((towrite-wrote) != 0){
                //file is full, the results that already had been wrote had been deleted, mark the file as done and then call again the writefile()
                status = 2;
            }
        }

        return status;
    }

    
    /**
     * Method to write the results into the resultsFile.
     * It clears each result of the ArrayList<AccelData> results immediatly after writting it.
     * It has a maximum of 8190 writes for each call.
     * If file reaches the MAX_LENGTH value, a new file will be created.
     * FileName format: 'ddMMAA_HH_X.txt'. 'X' value is the number of the file depending on the order it has been created.
     *
     * @return Boolean
     *      True on success, otherwise false.
     *
     * @throws IOException
     *      When failed to create, open or write a file or directory
     */
    public Boolean writeFile() throws IOException {
        Date now = null;
        String fileHour = "", filename = "";

        int id = -1, status = 0;
        Boolean success = false;

        File resultsFile = null;
        File resultsFolder = new File(Environment.getExternalStorageDirectory(), "WalkingHealth");

        if(createFolder(resultsFolder)) {
            now = new Date();
            fileHour = utils.getStringCurrentHour(now);

            utils.log("WriteFileManager","getting name");

            //Implementació del punt 2 del document: 'FunctionalDesign_WriteFileManager'
            filename = createFileName(now);
            resultsFile = new File(resultsFolder, filename);
            utils.log("WriteFileManager","name = "+filename);

            utils.log("WriteFileManager","exist "+filename+"?");
            id = WFMmodel.existFile(filename);
            utils.log("WriteFileManager","id = "+id);

            if (id == -1) {
                //if not exists, add to db and create it in the folder
                utils.log("WriteFileManager", "inserting newfile = " + filename);
                createFile(resultsFile);
                utils.log("WriteFileManager", "inserting newfile = " + filename);
                WFMmodel.insert_newFile(now, fileHour, filename);
                utils.log("WriteFileManager", "success inserting");

                status = writeTask(resultsFile);
            }
            else{
                //if the file exists, check if is full
                if (!isFull(resultsFile)) {
                    status = writeTask(resultsFile);
                }
                else{
                    //if is full, mark the file as done and rewrite the filename calling himself recursively .
                    utils.log("WriteFileManager","Set as done");
                    WFMmodel.done(id);
                    success = writeFile();
                }
            }

            switch (status){
                case 0:
                    success = false;
                    break;

                case 1:
                    success = true;
                    break;

                case 2:
                    WFMmodel.done(id);
                    success = writeFile();
                    break;
            }
        }

        return success;
    }
}