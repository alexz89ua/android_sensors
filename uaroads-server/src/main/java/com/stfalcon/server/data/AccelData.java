package com.stfalcon.server.data;

/**
 * Created by alex on 30.06.16.
 */
public class AccelData {
    public String device;
    public int sensorType;
    public float x;
    public float y;
    public float z;
    public float sqrt;
    public float verticalAcc;
    public long time;
    public double lat;
    public double lon;
    public double speed;


    public AccelData(String device, int sensorType, float x, float y, float z, float sqrt,
                     float verticalAcc, long time, double lat, double lon, double speed) {
        this.device = device;
        this.sensorType = sensorType;
        this.x = x;
        this.y = y;
        this.z = z;
        this.sqrt = sqrt;
        this.verticalAcc = verticalAcc;
        this.time = time;
        this.lat = lat;
        this.lon = lon;
        this.speed = speed;
    }
}
