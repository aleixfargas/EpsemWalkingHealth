package epsem.walkinghealth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends Activity implements BLEConnectionListener {
    public MainActivity main_activity = null;
    private Button btnConnect;
    public ArrayList<String> MACaddrArray = new ArrayList<>();
    //G2
    //public String MACaddr_right = "FE:15:A6:64:56:D2";

    // G1
    //public String MACaddr_right = "C7:C6:B3:FB:67:ED";

    //Our
    public String MACaddr_right = "D6:1A:9F:67:E5:8C";
    //public String MACaddr_right = "F8:08:97:8B:45:29";
    //public String MACaddr_right = "DD:81:3C:77:F6:52";

    public BLEConnection BleConnection = null;
    public static final Integer RADINO_RIGHT = 0;
    public static final Integer RADINO_LEFT = 1;

    public String connect_status = "Connect";
    public Integer status = 0;

    public String text = "";
    public Integer duration;
    public Context appcontext;

    private Intent nextScreen = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        appcontext = getApplicationContext();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        main_activity = this;
        this.MACaddrArray.add(RADINO_RIGHT, this.MACaddr_right);

        createConnectButton();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if(menu.hasVisibleItems()){}
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings){
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onResume(){
        super.onResume();
        this.BleConnection = BLEConnection.getInstance();
        this.BleConnection.addListener(this);
    }


    @Override
    public void onPause(){
        super.onPause();
        this.BleConnection = BLEConnection.getInstance();
        this.BleConnection.removeListener(this);
    }


    @Override
    public void onStatusUpdated(String MACaddr, Integer newStatus){
        switch (newStatus){
            case DEVICE_DISCONNECTED:
                status = 0;
                connect_status = "Connect";
                btnConnect.post(new Runnable() {
                    public void run() {
                        btnConnect.setText(connect_status);
                    }
                });

                text = "Device connection closed";
                duration = Toast.LENGTH_LONG;
                Toast.makeText(this.appcontext,this.text, this.duration);
                break;

            case DEVICE_CONNECTING:
                connect_status = "Connecting";
                btnConnect.post(new Runnable() {
                    public void run() {
                        btnConnect.setText(connect_status);
                    }
                });
                break;

            case DEVICE_CONNECTED:
                connect_status = "Disconnect";
                btnConnect.post(new Runnable() {
                    public void run(){
                        btnConnect.setText(connect_status);
                    }
                });

                this.BleConnection.removeListener(this);
                //Starting a new Intent
                nextScreen = new Intent(getApplicationContext(), GraphActivity.class);
                //startActivity(nextScreen);
                this.startActivityForResult(nextScreen, 1);
                break;

            case DEVICE_DISCONNECTING:
                connect_status = "Disconnecting";
                btnConnect.post(new Runnable() {
                    public void run() {
                        btnConnect.setText(connect_status);
                    }
                });
                break;

            default:
                //connection state unknown...
                break;
        }
    }

    public void onActivityResult(int status){
        if(status == 1){
            this.status = 0;
            connect_status = "Connect";
            btnConnect.post(new Runnable() {
                public void run() {
                    btnConnect.setText(connect_status);
                }
            });
        }
    }
    @Override
    public void onDataReceived(String MACaddr, ArrayList<AccelData> result, int batteryState) {
    }


    public void createConnectButton() {
        btnConnect = (Button) findViewById(R.id.connect);
        btnConnect.setText(connect_status);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Disconnect mode - BLE not connected and user wants to connect
                if (btnConnect.getText().equals("Connect") && status == 0) {
                    connect_status = "Connecting";
                    btnConnect.setText(connect_status);

                    if (BleConnection != null) {
                        //connecting to bluetooth
                        BleConnection.createCallback(RADINO_RIGHT, MACaddrArray.get(RADINO_RIGHT), appcontext, getSystemService(Context.BLUETOOTH_SERVICE));
                        BleConnection.BLEconnect(RADINO_RIGHT, main_activity);
                        status = 1;
                    }
                    else {
                        Log.e("onClick", "No BleConnection Object created!!");
                    }
                }
                //connection mode - BLE connection established and user wants to disconnect
                else if (btnConnect.getText().equals("Disconnect") && status == 1) {
                    BleConnection.BLEdisconnect(RADINO_RIGHT);
                    status = 0;
                }
                else if(btnConnect.getText().equals("")){
                    /*Unknown state, button has no name, so setting to disconnect mode*/
                    status = 0;
                    btnConnect.setText("Connect");
                }
            }
        });
    }
}