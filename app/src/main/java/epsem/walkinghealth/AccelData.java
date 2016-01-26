package epsem.walkinghealth;

import java.util.ArrayList;

public class AccelData {
    private String timestamp;
    public int sampleCounter;
    public double x;
    public double y;
    public double z;
    public int sensor;
    private int usuari;

    public AccelData(int sensor, String timestamp, int counter, double x, double y, double z) {
        this.timestamp = timestamp;
        this.sampleCounter = counter;
        this.x = x;
        this.y = y;
        this.z = z;
        this.usuari = 2;
        this.sensor = sensor;
    }

    public String getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    public int getSensor() {
        return sensor;
    }
    public void setSensor(int sensor) {
        this.sensor = sensor;
    }
    public long getUsuari() {
        return usuari;
    }
    public void setUsuari(int usuari) {
        this.usuari = usuari;
    }
    public String toString(){
        return usuari+","+sensor+","+timestamp+","+sampleCounter+","+x+","+y+","+z+"\n";
    }
}
