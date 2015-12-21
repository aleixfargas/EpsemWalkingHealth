package epsem.walkinghealth;

import java.util.ArrayList;

public interface BLEConnectionListener {
    /**
     * String representation of the Integer status
     * */
    public static final int DEVICE_DISCONNECTED = 0;
    public static final int DEVICE_CONNECTING = 1;
    public static final int DEVICE_CONNECTED = 2;
    public static final int DEVICE_DISCONNECTING = 3;


    /**
     * BLEConnection will launch this method when the bluetooth connection status change.
     *
     * @param MACaddr The MAC address of the device
     * @param newStatus The new status of the connection.
     * @value Integer representation
     * */
    void onStatusUpdated(String MACaddr, Integer newStatus);


    /**
     * BLEConnection will launch this method when we receive some data from the remote device.
     *
     * @param MACaddr MAC address of the remote device that send a characteristic
     * @param results Array of the type AccelData that contains the results received at the moment
    */
    void onDataReceived(String MACaddr, ArrayList<AccelData> results);

}
