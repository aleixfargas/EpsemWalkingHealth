package epsem.walkinghealth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BLEConnection {
    public BluetoothManager manager;
    public BluetoothAdapter adapter;
    public BluetoothDevice device;
    public BluetoothGatt gatt;
    public BluetoothGattCallback callback;

    public String MACaddr;
    private ArrayList<AccelData> results = new ArrayList<>();

    //Toast --> http://developer.android.com/guide/topics/ui/notifiers/toasts.html
    public Context appcontext;
    public CharSequence text = "connecting to device";
    public int duration;
    private String connect_status = "Connect";

    public BLEConnection(final String MACaddr, final boolean started, final GraphChart graph, final Object systemService, final Context appcontext, final Thread connectThread) {

        this.MACaddr = MACaddr;

        callback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.e("BLE", "newState = " + newState);
                switch(newState){
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
                connectThread.run();
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

                    //Visualització dades
                    graph.add(System.currentTimeMillis(), (double)(data[0]), (double)(data[1]), (double)(data[2]));
                    graph.update();
                }
            }
        };

        this.manager = (BluetoothManager) systemService;
        this.adapter = this.manager.getAdapter();
        this.device = this.adapter.getRemoteDevice(MACaddr);
        this.gatt = this.device.connectGatt(appcontext, false, this.callback);
    }

    public void enableTXNotification() {
        final UUID UART_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
        final UUID TX_CHAR = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
        final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

        BluetoothGattService UartService = this.gatt.getService(UART_SERVICE);
        if (UartService != null) {
            BluetoothGattCharacteristic TxChar = UartService.getCharacteristic(TX_CHAR);
            if (TxChar != null) {
                this.gatt.setCharacteristicNotification(TxChar, true);
                BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                this.gatt.writeDescriptor(descriptor);
            }
        }
    }

    public boolean BLEconnect() {
        //List<BluetoothDevice> ble_array = this.manager.getConnectedDevices(this.gatt);
        if (this.device == null) {
            Log.w("Ap", "Device not found.  Unable to connect.");
            return false;
        }
        return true;
    }

    public boolean BLEdisconnect() {
        if (this.adapter != null && this.gatt != null) {
            this.gatt.disconnect();

            this.manager = null;
            this.adapter = null;
            this.device = null;
            this.gatt = null;
            return true;
        } else {
            return false;
        }
    }

    public ArrayList<AccelData> getResults(){
        return this.results;
    }

    public String getStatus(){
        return this.connect_status;
    }

    public void clearResults(){
        this.results.clear();
    }
}
