package epsem.walkinghealth;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.UUID;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

public class MainActivity extends Activity {

    public BluetoothManager manager;
    public BluetoothAdapter adapter;
    public BluetoothDevice device;
    public BluetoothGatt gatt;
    public BluetoothGattCallback callback;
    private ArrayList<AccelData> results = new ArrayList<>();;

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

    private Button btnConnect, btnStartStop;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnConnect = (Button) findViewById(R.id.connect);
        btnStartStop = (Button) findViewById(R.id.startstop);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnConnect.getText().equals("Connect")) {
                    btnConnect.setText("Disconnect");
                }
                else{
                    btnConnect.setText("Connect");
                }
            }
        });
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
        ble();
        connect("DB:0B:C0:B1:0D:38");
        disconnect();
    }

    public boolean connect(final String address) {

        final BluetoothDevice device = adapter.getRemoteDevice(address);
        if (device == null) {
            //Log.w("Ap", "Device not found.  Unable to connect.");
            return false;
        }
        gatt = device.connectGatt(getApplicationContext(), false, callback);
        return true;
    }

    public void disconnect() {
        if (adapter != null && gatt != null) {
            gatt.disconnect();
        }
    }

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
                //characteristic.getValue();
            }
        };

        this.manager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        this.adapter = manager.getAdapter();
        this.device = adapter.getRemoteDevice("DB:0B:C0:B1:0D:38");
        this.gatt = device.connectGatt(this, false, callback);
    }
}

