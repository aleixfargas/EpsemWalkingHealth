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

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class GraphActivity extends Activity implements BLEConnectionListener {
    public Button btnClearGraph;
    public GraphChart graph = null;
    public BLEConnection BleConnection = null;
    public WriteFileManager writeFileManager = null;
    public ArrayList<AccelData> results;

    private static final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        createGraph();

        createClearGraphButton();
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

        writeFileManager = new WriteFileManager(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        BleConnection = BLEConnection.getInstance();
        BleConnection.removeListener(this);
        writeFileManager=null;
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
    public void onDataReceived(String MACaddr, AccelData result) {
        Log.e("GraphActivity", "Data received from " + MACaddr);
        Double x = results.get(results.size()).x;
        Double y = results.get(results.size()).y;
        Double z = results.get(results.size()).z;

        graph.toString();
        graph.add(System.currentTimeMillis(), x, y, z);
        graph.update();

        this.results.add(result);
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
            }
        });
    }

    public void upload() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getActiveNetworkInfo();

        if (mWifi.isConnected()) {
            ServerUploader upload = new ServerUploader();
            upload.execute();
        }
    }


    /**
     * Method called from WriteFileManager, in order to write in the file every minute
     *
     * @return ArrayList<AccelData> contains all the characteristics received
     */
    public ArrayList<AccelData> getResults(){
        return this.results;
    }


    /**
     * Clear all the data received from the moment
     */
    public void clearResults(){
        this.results.clear();
    }
}