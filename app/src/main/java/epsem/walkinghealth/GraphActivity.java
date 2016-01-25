package epsem.walkinghealth;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.os.FileObserver;
import android.widget.TextView;

import epsem.walkinghealth.common.utils;

public class GraphActivity extends Activity implements BLEConnectionListener {

    public Button btnClearGraph;
    public TextView batteryLevel;
    public GraphChart graph = null;
    public BLEConnection BleConnection = null;
    public WriteFileManager writeFileManager = null;
    public ServerUploader upload = null;
    public Boolean locked = false;
    private ArrayList<AccelData> results = new ArrayList<>();
    private static final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private int num = 0;

    public static final Integer RADINO_RIGHT = 0;
    public static final Integer RADINO_LEFT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        createGraph();

        createClearGraphButton();

        Runnable task = new Runnable() {
            public void run() {
                graph.clear();
            }
        };
        worker.scheduleAtFixedRate(task, 30, 30, TimeUnit.SECONDS);

        this.batteryLevel = (TextView) findViewById(R.id.batterylevel);

        this.writeFileManager = new WriteFileManager(this);
        this.upload = new ServerUploader(this);
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
        if (newStatus == DEVICE_DISCONNECTED) {
            //finish graph_activity, BLE device disconnected
            BleConnection.removeListener(this);
            this.finish();
        }
    }

    @Override
    public void onDataReceived(String MACaddr, ArrayList<AccelData> result, int batteryState) {
        for(int i = 0; i < 6; i++){
            Double x = result.get(i).x;
            Double y = result.get(i).y;
            Double z = result.get(i).z;

            if(!locked) {
                utils.log("GraphActivity", "writting " + result.toString());
                this.results.add(result.get(i));
            }

            updateBattery(batteryState);

            graph.toString();
            graph.add(System.currentTimeMillis(), x, y, z);
            graph.update();
        }
    }


    public void updateBattery(int level){
        batteryLevel.setText(level+"%");
    }

    public void createGraph() {
        android.widget.LinearLayout layout;

        graph = new GraphChart(getBaseContext());
        layout = (LinearLayout) findViewById(R.id.graph_layout);
        layout.addView(graph.getView());
    }


    public void createClearGraphButton() {
        btnClearGraph = (Button) findViewById(R.id.cleargraph);

        btnClearGraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                graph.clear();
                graph.update();
            }
        });
    }


    public void upload(){
        Runnable task = new Runnable() {
            public void run() {
                ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo mWifi = connManager.getActiveNetworkInfo();
                if (mWifi.isConnected()) {
                    upload.execute();
                }
            }
        };
        worker.scheduleAtFixedRate(task, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Method called from WriteFileManager, to concat the results with his own results ArrayList
     *
     * @return ArrayList<AccelData> contains all the characteristics received at the moment
     */
    public ArrayList<AccelData> getResults(){
        ArrayList<AccelData> results_copy = null;

        this.locked = true;
        results_copy = new ArrayList<>(this.results);
        this.locked = false;

        return results_copy;
    }


    /**
     * Clear all the data received from the moment
     */
    public void clearResults(){
        this.locked = true;
        this.results.clear();
        this.locked = false;
    }
}