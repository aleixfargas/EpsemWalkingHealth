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
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.widget.LinearLayout;

public class MainActivity extends Activity {
    public boolean started;
    public String MACaddr = "F5:8B:DF:1F:95:B0";
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createConnectButton();
        createGraph();
        createStartButton();
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

        this.graph = new GraphChart(getBaseContext());
        this.graph.clear();
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
                    btnStartStop.setText("Stop");
                } else {
                    btnStartStop.setText("Start");
                }
            }
        });
    }

//----------------END START/STOP FUNCTIONS----------------
// ----------------START WRITE FILE FUNCTIONS-------------

    private void writeFile(){

        File sdCard = Environment.getExternalStorageDirectory();
        File file = new File(sdCard, "mybackup.txt");
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
                    Log.d("BLE","connection stablished");
                }
                else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("BLE","connection losed");
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
                    byte[] data = characteristic.getValue();
                    //writeFile();
                    graph.add(System.currentTimeMillis(), toDouble(data));
                    graph.update();
                }
            }

            public double toDouble(byte[] bytes){
                return ByteBuffer.wrap(bytes).getDouble();
            }
        };

        this.manager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        this.adapter = manager.getAdapter();
        this.device = adapter.getRemoteDevice(this.MACaddr);
        this.gatt = device.connectGatt(this, false, callback);
    }
}

