package epsem.walkinghealth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static android.support.v4.app.ActivityCompat.startActivityForResult;

public class BLEConnection {
    private static BLEConnection singletonInstance = new BLEConnection();

    public static final Integer RADINO_RIGHT = 0;
    public static final Integer RADINO_LEFT = 1;
    private final static int REQUEST_ENABLE_BT = 1;

    private ArrayList<BLEConnectionListener> listeners = new ArrayList<>();
    private ArrayList<AccelData> results = new ArrayList<>();

    public ArrayList<BluetoothGattCallback> CallbackArray = new ArrayList<>();
    public ArrayList<BluetoothManager> ManagerArray = new ArrayList<>();
    public ArrayList<BluetoothAdapter> AdapterArray = new ArrayList<>();
    public ArrayList<BluetoothDevice> DeviceArray = new ArrayList<>();
    public ArrayList<BluetoothGatt> GattArray = new ArrayList<>();
    public ArrayList<String> MACaddrArray = new ArrayList<>();

    public Object SystemService = null;

    public Integer DeviceNumber = 0;


    /**
     * Constructor cannot be called from outside
     */
    private BLEConnection(){}


    /**
     * Called in order to get the unique Instance of the BLEConnection class
     *
     * @return singletonInstance The unique object that identifies BLEConnection Class
     */
    public static BLEConnection getInstance() {
        return singletonInstance;
    }


    /**
     * Each time our app is resumed, all the Activities call this method to create a new listener to the BLEConnection class
     *
     * @param listener
     */
    public void addListener(BLEConnectionListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }


    /**
     * Each time our app is stopped, all the Activities call this method to remove the current listener of the calling activity
     *
     * @param listener
     */
    public void removeListener(BLEConnectionListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }


    public void forwardStatusUpdate(String MACaddr, Integer status) {
        for (BLEConnectionListener listener: listeners) {
            listener.onStatusUpdated(MACaddr, status);
        }
    }


    public void forwardDataReceived(String MACaddr, AccelData result) {
        for (BLEConnectionListener listener: listeners) {
            listener.onDataReceived(MACaddr, result);
        }
    }


    /**
     * Method to create a new bluetooth callback, we can create as callbacks as devices we have.
     *
     *  @param id
     * @param MACaddr
     * @param appcontext
     * @param systemService
     */
    public void createCallback(final Integer id, final String MACaddr, final Context appcontext, final Object systemService){
        BluetoothGattCallback callback;
        BluetoothManager manager;
        BluetoothAdapter adapter;
        final BluetoothDevice device;
        BluetoothGatt gatt;

        this.SystemService = systemService;

        callback = new BluetoothGattCallback(){
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.e("BLE", "newState = " + newState);
                if(newState == BluetoothProfile.STATE_CONNECTED){
                    //S'ha establert la connexió amb el perifèric
                    gatt.discoverServices();
                }
                forwardStatusUpdate(MACaddrArray.get(id), newState);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                // S'han descobert els serveis del perifèric
                enableTXNotification(id);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                // S'ha rebut una notificació, el seu valor s'obté amb characteristic.getValue();
                //Rebuda de dades
                byte[] data = characteristic.getValue();
                Log.e("onCharChanged", "dades radino: [" + (double) (data[0]) + ", " + (double) (data[1])+ ", " + (double) (data[2]) + "]");//toDouble(data));

                // Processament de dades
                AccelData AD = new AccelData(id, System.currentTimeMillis(), data[0], data[1], data[2]);

                //Visualització dades + Emmagatzematge de dades
                forwardDataReceived(MACaddrArray.get(id), AD);
            }
        };
        Log.e("callback","callback #"+id+" created");

        /*Saving the callback and all BLE elements in an array with an id identifier*/
        this.MACaddrArray.add(id, MACaddr);
        manager = (BluetoothManager) this.SystemService;
        this.ManagerArray.add(id,manager);
        adapter = manager.getAdapter();
        this.AdapterArray.add(id,adapter);
        device = adapter.getRemoteDevice(MACaddr);
        this.DeviceArray.add(id,device);
        gatt = device.connectGatt(appcontext, false, callback);
        this.GattArray.add(id,gatt);
        this.CallbackArray.add(id,callback);

        this.DeviceNumber++;
    }


    public void enableTXNotification(Integer id) {
        final UUID UART_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
        final UUID TX_CHAR = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
        final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

        BluetoothGattService UartService = this.GattArray.get(id).getService(UART_SERVICE);
        if (UartService != null) {
            BluetoothGattCharacteristic TxChar = UartService.getCharacteristic(TX_CHAR);
            if (TxChar != null){
                this.GattArray.get(id).setCharacteristicNotification(TxChar, true);
                BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                this.GattArray.get(id).writeDescriptor(descriptor);
            }
        }
    }

/*
    public List getAvailableDevices(){
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        List<String> s= new ArrayList<>();

        Integer counter=0;
        for(BluetoothDevice bt : pairedDevices) {
            s.add(counter,bt.getName());
            counter++;
        }

        return s;
    }
*/

    /**
     * Method to know if Bluetooth is enabled, if not a popup will be showed
     *
     * @param id
     * @param activity
     */
    public void BLEconnect(Integer id, Activity activity) {
        BluetoothManager btmanager = (BluetoothManager) this.SystemService;
        BluetoothAdapter btAdapter = btmanager.getAdapter();
        if (this.AdapterArray.get(id) != null && !this.AdapterArray.get(id).isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(activity, enableIntent, REQUEST_ENABLE_BT, null);
        }
        forwardStatusUpdate(MACaddrArray.get(id),0);
    }


    /**
     * Method to disconnect from the bluetooth device
     *
     * @return boolean  True if successful, false if already disconnected
     */
    public boolean BLEdisconnect(Integer id){
        if (this.AdapterArray.get(id) != null && this.GattArray.get(id) != null) {
            this.GattArray.get(id).disconnect();

            this.ManagerArray.add(id, null);
            this.AdapterArray.add(id,null);
            this.DeviceArray.add(id, null);
            this.GattArray.add(id,null);
            return true;
        }
        else{
            return false;
        }
    }
}
