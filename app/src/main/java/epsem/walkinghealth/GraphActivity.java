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

public class GraphActivity extends Activity implements BLEConnectionListener {
    public Button btnClearGraph;
    public GraphChart graph = null;
    public BLEConnection BleConnection = null;
    public WriteFileManager writeFileManager = null;
    private ArrayList<AccelData> results = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        createGraph();

        createClearGraphButton();
        this.writeFileManager = new WriteFileManager(this);
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
    public void onDataReceived(String MACaddr, AccelData result) {
        Double x = result.x;
        Double y = result.y;
        Double z = result.z;
        Log.e("GraphActivity", "Data received from " + MACaddr);

        this.results.add(result);

        graph.toString();
        graph.add(System.currentTimeMillis(), x, y, z);
        graph.update();
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
        Log.e("WriteFileManager","Getting GraphA results");
        return this.results;
    }


    /**
     * Clear all the data received from the moment
     */
    public void clearResults(){
        this.results.clear();
    }
}