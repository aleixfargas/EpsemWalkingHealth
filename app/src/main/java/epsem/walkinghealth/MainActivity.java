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
import java.security.cert.TrustAnchor;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.widget.LinearLayout;

//popup imports
import android.app.Dialog;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    public boolean started = false;
    public String MACaddr = "F8:08:97:8B:45:29";

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

    //Toast --> http://developer.android.com/guide/topics/ui/notifiers/toasts.html
    public Context appcontext;
    CharSequence text = "connecting to device";
    public int duration;
    public String connect_status="Connect";

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

        createConnectButton();
        createGraph();
        createStartButton();
        ble();
        //is able to writte files? popup
        switch (checkExternalMedia()){
            case 0:
                Log.e("permissions","ERROR! 0");
                text = "Permission Writte Error";
                Toast.makeText(appcontext, text, duration).show();
                break;

            case -1:
                Log.e("permissions","ERROR! -1");
                break;

            default:
                Log.e("permissions","OK");
        }
        new ServerUploader().execute();
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

        btnConnect.setText(connect_status);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnConnect.getText().equals("Connect")) {
                    connect();
                } else {
                    disconnect();
                }
                btnConnect.setText(connect_status);
                Toast.makeText(appcontext, text, duration).show();
            }
        });

        //        adapter = BluetoothAdapter.getDefaultAdapter();
//
//        btnConnect.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!adapter.isEnabled()) {
//                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
//                }
//                else{
//                    if (btnConnect.getText().equals("Connect")){
//
//                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices
//
//                        Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
//                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
//                    }
//                    else {
//                        //Disconnect button pressed
//                        if (device != null) {
//                            mService.disconnect();
//                        }
//                    }
//        });
//    }
    }

    public boolean connect() {
        //List<BluetoothDevice> ble_array = this.manager.getConnectedDevices();
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
                    graph.clear();
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

    private BufferedWriter NewFileCreation(File file){
        BufferedWriter newFile_bw = null;
        try {
            if(file.createNewFile()){
                Log.e("new File","Success Creating");
                FileWriter fw = new FileWriter(file);
                newFile_bw = new BufferedWriter(fw);
            }
            else {
                Log.e("new File","error Creating");
            }
        } catch (Exception IOException) {
            Log.e("new writter error", "Cannot create new file");
        }

        return newFile_bw;
    }

    private BufferedWriter checkFolderFiles(File newFolder, String filename){
        BufferedWriter file_output = null;

        File[] listOfFiles = newFolder.listFiles();
        for (File f : listOfFiles) {
            Log.e("Checking Folder","founded file: "+f);
            if (f.isFile()) {
                Log.e("Checking Folder",f+" is file and his name is: "+f.getName());
                if (f.getName().equals(filename)){
                    Log.e("Checking Folder",f.getName()+" == "+filename);
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
        int i=0;
        BufferedWriter output;

        //return the current date String formatted
        String now = getStringDateTime();
        //Define here the name of the file
        String filename = now+"_data.txt";

        File newFolder = new File(Environment.getExternalStorageDirectory(), "WalkingHealth");
        Log.e("Folder","new Folder: "+newFolder);

        if (!newFolder.exists()) {
            Log.e("Folder", "creating...");
            if(newFolder.mkdirs()){
                Log.e("Folder","Success Creating");
            }
            else {
                Log.e("Folder","error Creating");
            }
        }

        //checking Folder in order to find if we have the same datetime file if founded, create a new FileWritter
        output = checkFolderFiles(newFolder, filename);

        if(output == null) {
            //file not exists, so we create it and create a new FileWritter
            File file = new File(newFolder, filename);
            output = NewFileCreation(file);
        }

        try {
            for (i=0;i<results.size();i++);
            output.write((results.get(i-1)).toString());

            if (output != null) {
                output.flush();
                output.close();
            }
        } catch (Exception IOException) {
            Log.e("Write in file", "Exception: " + IOException.getMessage());
        }
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
                Log.e("BLE", "newState = " + newState);
                switch (newState){
                    case 0:
                        connect_status="Connect";
                        text = "device disconnected";
                        duration = Toast.LENGTH_LONG;
                        break;
                    case 1:
                        connect_status="Connecting";
                        text = "connecting to device";
                        duration = Toast.LENGTH_SHORT;
                        break;
                    case 2:
                        connect_status="Disconnect";
                        text = "device connected";
                        duration = Toast.LENGTH_LONG;
                        gatt.discoverServices();
                        break;
                    case 3:
                        connect_status="Disconnecting";
                        text = "disconnecting device";
                        duration = Toast.LENGTH_SHORT;
                        break;
                    default:
                        text = "connection state unknown...";
                        duration = Toast.LENGTH_LONG;
                        break;
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
                    graph.add(System.currentTimeMillis(), (double)(data[0]), (double)(data[1]), (double)(data[2]));
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

