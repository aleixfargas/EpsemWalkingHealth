package epsem.walkinghealth;

import java.util.ArrayList;

public class AccelData {
    private long timestamp;
    private double x;
    private double y;
    private double z;
    private int usuari;
    private int sensor;

    public AccelData(int sensor, long timestamp, double x, double y, double z) {
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.z = z;
        this.usuari = 1;
        this.sensor = sensor;
    }

    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String toString(){
        return timestamp+","+sensor+","+x+","+y+","+z+";";
    }
}
