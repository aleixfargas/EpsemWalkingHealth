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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.widget.LinearLayout;

//popup imports
import android.app.Dialog;
import android.widget.TextView;

public class MainActivity extends Activity {
    public boolean started = false;
    //public String MACaddr = "F5:8B:DF:1F:95:B0";
    public String MACaddr = "DB:0B:C0:B1:0D:38";
    //    public GraphChart graph = new GraphChart(getBaseContext());
    public GraphChart graph;
    public BluetoothManager manager;
    public BluetoothAdapter adapter;
    public BluetoothDevice device;
    public BluetoothGatt gatt;
    public BluetoothGattCallback callback;

    private ArrayList<AccelData> results = new ArrayList<>();
    private boolean connection_status = false;
    private Button btnConnect, btnStartStop;
    //Popup variables see: http://developer.android.com/guide/topics/ui/dialogs.html
    /*
    public Context context = new Context();
    public Dialog dialog = new Dialog(context);
    public TextView txt = (TextView)dialog.findViewById(R.id.textbox);
    */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createConnectButton();
        createGraph();
        createStartButton();

        //is able to writte files? popup
        switch (checkExternalMedia()){
            case 0:
                //txt.setText(getString(R.string.message));
                break;
            case -1:
                //txt.setText(getString(R.string.message));
                break;
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

    public void createConnectButton(){
        btnConnect = (Button) findViewById(R.id.connect);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (btnConnect.getText().equals("Connect")) {
                if (connect()){
                    btnConnect.setText("Disconnect");
                }
                else{
                    btnConnect.setText("Failed connection"); //Pop up!
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            else {
                if(disconnect()) {
                    btnConnect.setText("Connect");
                }
                else{
                    Log.e("Disconnect","NOT DISCONNECTED");
                }
            }
            }
        });
    }

    public boolean connect() {
        ble();
        if (this.device == null) {
            Log.w("Ap", "Device not found.  Unable to connect.");
            return false;
        }
        return true;
    }

    public boolean disconnect() {
        if (this.adapter != null && this.gatt != null) {
            this.gatt.disconnect();
            return true;
        }
        else{
            return false;
        }
    }

//----------------END CONNECT BUTTON FUNCTIONS----------------
//----------------START GRAPH FUNCTIONS----------------

    public void createGraph(){
        android.widget.LinearLayout layout;

        graph = new GraphChart(getBaseContext());
        graph.clear();
        layout = (LinearLayout) findViewById(R.id.graph_layout);
        layout.addView(this.graph.getView());
    }

//----------------END GRAPH FUNCTIONS----------------
//----------------START START/STOP FUNCTIONS----------------

    public void createStartButton(){
        btnStartStop = (Button) findViewById(R.id.startstop);

        btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnStartStop.getText().equals("Start")) {
                    started = true;
                    btnStartStop.setText("Stop");
                } else {
                    started = false;
                    btnStartStop.setText("Start");
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
        Log.e("Podem escriure/llegir?","External Media: readable="+mExternalStorageAvailable+" writable="+mExternalStorageWriteable);
        return result;
    }

    private String getStringDateTime(){
        // (1) get today's date
        Date today = Calendar.getInstance().getTime();
        // (2) create a date "formatter" (the date format we want)
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        // (3) create a new String using the date format we want
        return formatter.format(today);
    }

    private BufferedWriter createNewFile(File newFolder, File file, String now) {
        BufferedWriter newFile_output = null;

        Log.e("exists", "The file no exists so we create it");
        file = new File(newFolder, now + "_data.txt");
        try{
            file.createNewFile();
            newFile_output = new BufferedWriter(new FileWriter(file));
        }
        catch(Exception IOException){
            Log.e("new file error", "Cannot create newFile");
        }

        return newFile_output;
    }

    private BufferedWriter checkFolderFiles(File newFolder, String now){
        BufferedWriter file_output = null;

        File[] listOfFiles = newFolder.listFiles();
        for (File f : listOfFiles) {
            if (f.isFile()) {
                if (f.getName() == now+"_data.txt"){
                    try {
                        file_output = new BufferedWriter(new FileWriter(f,true));
                    } catch (Exception IOException) {
                        Log.e("old file error","Cannot open oldFile");
                    }
                }
            }
        }

        return file_output;
    }

    private void writeFile(){
        Writer output = null;
        File file = null;

        //return the current date String formatted
        String now = getStringDateTime();

        File newFolder = new File(Environment.getExternalStorageDirectory(), "WalkingHealth");
        if (!newFolder.exists()) {
            newFolder.mkdir();
        }

        //checking Folder in order to find if we have the same datetime file if founded, create a new FileWritter
        output = checkFolderFiles(newFolder, now);
        if(output != null) {
            //file not exists, so we create it and create a new FileWritter
            output = createNewFile(newFolder, file, now);
        }

        try {
            for (AccelData data : results) {
                output.write(data.toString());
            }
            if (output != null) {
                output.flush();
                output.close();
            }
        } catch (Exception e) {
            Log.e("Write in file", "Exception: " + e.getMessage());
        }

        /*
        ------- OLD -------
        File sdCard = Environment.getExternalStorageDirectory();
        //String WH = sdCard + "/WalkingHealth";
        Log.e("SD FileDir","sdCard location: "+sdCard);
        File file = new File(sdCard + "/WalkingHealth", "mybackup.txt");
        //file.mkdirs();
        Writer output = null;

        try {
            output = new BufferedWriter(new FileWriter(file));
            for (AccelData data : results){
                output.write(data.toString());
            }
            if (output != null){
                output.close();
            }
        }
        catch (Exception e){
            Log.d("MyAct", "Exception: " + e.getMessage());
        }
        -------------------
        */
    }

// ----------------END WRITE FILE FUNCTIONS-------------

    public void enableTXNotification() {
        final UUID UART_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
        final UUID TX_CHAR = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
        final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

        BluetoothGattService UartService = gatt.getService(UART_SERVICE);
        if (UartService != null) {
            BluetoothGattCharacteristic TxChar = UartService.getCharacteristic(TX_CHAR);
            if (TxChar != null) {
                gatt.setCharacteristicNotification(TxChar, true);
                BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }
        }
    }

    public void ble() {
        callback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // S'ha establert la connexió amb el perifèric
                    gatt.discoverServices();
                    Log.d("BLE","connection established");
                }
                else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("BLE","connection lost");
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                // S'han descobert els serveis del perifèric
                enableTXNotification();
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                // S'ha rebut una notificació, el seu valor s'obté amb characteristic.getValue();
                if (started){
                    //Rebuda de dades
                    byte[] data = characteristic.getValue();
                    Log.e("onCharChanged", "dades radino: [" + (double) (data[0]) + ", " + (double) (data[1])+ ", " + (double) (data[2]) + "]");//toDouble(data));

                    // Processament de dades
                    AccelData AD = new AccelData(1, System.currentTimeMillis(), data[0], data[1], data[2]);

                    //Emmagatzematge de dades
                    results.add(AD);
                    writeFile();

                    //Visualització dades
                    graph.add(System.currentTimeMillis(), (double)(data[0]));
                    graph.update();
                }
            }
        };

        this.manager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        this.adapter = manager.getAdapter();
        this.device = adapter.getRemoteDevice(this.MACaddr);
        this.gatt = device.connectGatt(this, false, callback);
    }
}

