package epsem.walkinghealth;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.CountDownLatch;


public class MainActivity extends Activity {
    public boolean started = false;

    private Button btnConnect, btnStartStop;
    public GraphChart graph;
    //public String MACaddr_right = "F8:08:97:8B:45:29";
    public String MACaddr_right = "C3:EE:DD:D5:E8:CB";
    //public String MACaddr_right = "DD:81:3C:77:F6:52";

    public BLEConnection radino_right = null, radino_left = null;
    public Thread connectThread = null;

    private ArrayList<AccelData> results = new ArrayList<>();
    public String connect_status = "Connect";
    private Integer status = 0;

    //Toast --> http://developer.android.com/guide/topics/ui/notifiers/toasts.html
    public Context appcontext;
    public CharSequence text = "connecting to device";
    public int duration;

    //Popup variables see: http://developer.android.com/guide/topics/ui/dialogs.html
    /*
    public Context context = new Context();
    public Dialog dialog = new Dialog(context);
    public TextView txt = (TextView)dialog.findViewById(R.id.textbox);
    */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        appcontext = getApplicationContext();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createGraph();

        createConnectButton();
        createStartButton();

        //is able to writte files? popup
        switch (checkExternalMedia()) {
            case 0:
                Log.e("permissions", "ERROR! 0");
                text = "Permission Writte Error";
                Toast.makeText(appcontext, text, duration).show();
                break;

            case -1:
                Log.e("permissions", "ERROR! -1");
                break;

            default:
                Log.e("permissions", "OK");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

//----------------START CONNECT BUTTON FUNCTIONS----------------

    public void createConnectButton() {
        final CountDownLatch latch = new CountDownLatch(1);
        btnConnect = (Button) findViewById(R.id.connect);
        btnConnect.setText(connect_status);

        connectThread = new Thread(new Runnable() {
            @Override
            public void run(){
                if (radino_right != null) {
                    Log.e("thread","radino_status="+radino_right.getStatus());
                    if (!connect_status.equals(radino_right.getStatus())) {
                        Log.e("thread","connect_status!=getStatus()");
                        connect_status = radino_right.getStatus();
                        if (connect_status.equals("Disconnect")) {
                            status = 1;
                        } else {
                            radino_right = null;
                            status = 0;
                        }
                        btnConnect.post(new Runnable() {
                            public void run() {
                                btnConnect.setText(connect_status);
                            }
                        });
                        //btnConnect.setText(connect_status);

                    }
                }
                latch.countDown();
            }
        });

        connectThread.start();

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Disconnect mode - BLE not connected and user wants to connect
                if (btnConnect.getText().equals("Connect") && status == 0) {
                    radino_right = new BLEConnection(MACaddr_right, started, graph, getSystemService(Context.BLUETOOTH_SERVICE), appcontext, connectThread);
                    radino_right.BLEconnect();
                    status=1;
                    connect_status = "Connecting";
                    btnConnect.setText("Connecting");
                }
                //connection mode - BLE connection established and user wants to disconnect
                else if(btnConnect.getText().equals("Disconnect") && status == 1){
                    radino_right.BLEdisconnect();
                    radino_right=null;

                    btnConnect.setText("Connect");
                    btnStartStop.setText("Start");

                    started = false;
                    status=0;
                }

                else{
                    /*Unknown state,maybe connecting, so do nothing*/
                }
            }
        });
    }
//----------------END CONNECT BUTTON FUNCTIONS----------------
//----------------START GRAPH FUNCTIONS----------------

    public void createGraph() {
        android.widget.LinearLayout layout;

        this.graph = new GraphChart(getBaseContext());
        layout = (LinearLayout) findViewById(R.id.graph_layout);
        layout.addView(this.graph.getView());
    }

//----------------END GRAPH FUNCTIONS----------------
//----------------START START/STOP FUNCTIONS----------------

    public void createStartButton() {
        btnStartStop = (Button) findViewById(R.id.startstop);

        btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (status == 1) {
                    if (btnStartStop.getText().equals("Start")) {
                        started = true;
                        graph.clear();
                        btnStartStop.setText("Stop");
                    }
                    else {
                        started = false;
                        writeFile();
                        btnStartStop.setText("Start");
                    }
                }
            }
        });
    }

//----------------END START/STOP FUNCTIONS----------------
// ----------------START WRITE FILE FUNCTIONS-------------

    private Integer checkExternalMedia() {
        Integer result = 0;
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
            result = 1;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
            result = -1;
        } else {
            // Can't read or write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
        Log.e("Podem escriure/llegir?", "External Media: readable=" + mExternalStorageAvailable + " writable=" + mExternalStorageWriteable);
        return result;
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
            this.results = this.radino_right.getResults();
            for(i = 0; i<this.results.size(); i++) {
                output.write(this.results.get(i).toString());
            }
            if (output != null) {
                output.flush();
                output.close();
                this.radino_right.clearResults();
            }
        } catch (Exception IOException) {
            Log.e("Write in file", "Exception: " + IOException.getMessage());
        }
    }
}