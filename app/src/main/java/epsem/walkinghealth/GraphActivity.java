package epsem.walkinghealth;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class GraphActivity extends Activity implements BLEConnectionListener{
    public Button btnClearGraph;
    public GraphChart graph = null;
    private MainActivity main_activity = null;
    public BLEConnection BleConnection = null;

    public ArrayList<AccelData> results = null;
    //public GraphActivity graph_activity = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        createGraph();

        createClearGraphButton();
        upload();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_graph, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        BleConnection = BLEConnection.getInstance();
        BleConnection.addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        BleConnection = BLEConnection.getInstance();
        BleConnection.removeListener(this);
    }

    @Override
    public void onStatusUpdated(String MACaddr, Integer newStatus) {
        if(newStatus == DEVICE_DISCONNECTED){
            //finish graph_activity, BLE device disconnected
            BleConnection.removeListener(this);
            this.finish();
        }
    }

    @Override
    public void onDataReceived(String MACaddr, double x, double y, double z){
        Log.e("GraphActivity","Data received from " + MACaddr);

        graph.toString();
        graph.add(System.currentTimeMillis(), x, y, z);
        graph.update();
    }

//----------------START GRAPH FUNCTIONS----------------

    public void createGraph(){
        android.widget.LinearLayout layout;

        graph = new GraphChart(getBaseContext());
        layout = (LinearLayout) findViewById(R.id.graph_layout);
        layout.addView(graph.getView());
    }

//----------------END GRAPH FUNCTIONS----------------
//----------------START ClearGraph FUNCTIONS----------------

    public void createClearGraphButton() {
        btnClearGraph = (Button) findViewById(R.id.cleargraph);

        btnClearGraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                graph.clear();
                writeFile();
            }
        });
    }

//----------------END START/STOP FUNCTIONS----------------
// ----------------START WRITE FILE FUNCTIONS-------------
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
            }
            else{
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
                    } catch (Exception IOException) {
                        Log.e("old file error", "Cannot open oldFile");
                    }
                }
            }
        }

        return file_output;
    }

    private void writeFile() {
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
            else {
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
            this.results = this.BleConnection.getResults();
            for(i = 0; i<this.results.size(); i++) {
                output.write(this.results.get(i).toString());
            }
            if (output != null) {
                output.flush();
                output.close();
                this.BleConnection.clearResults();
            }
        } catch (Exception IOException) {
            Log.e("Write in file", "Exception: " + IOException.getMessage());
        }
    }

    public void upload(){
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getActiveNetworkInfo();

        if (mWifi.isConnected()) {
            ServerUploader upload = new ServerUploader();
            upload.execute();
        }
    }
}
