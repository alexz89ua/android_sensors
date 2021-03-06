package com.stfalcon.client;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.stfalcon.client.connection.ConnectionWrapper;

/**
 * @author alwx
 * @version 1.0
 */
public class MyApplication extends Application {
    public static final String CONNECTED = "com.example.sasha.CONNECTED";
    public static final String DATA = "com.example.sasha.DATA";
    public static final String DEVICE = "device";
    public static final String SPEED = "speed";
    public static final String WIFI = "wifi";
    public static final String STARTED = "started";
    public static final String SENSOR = "sensor";
    public static final String SENSOR_TYPE = "sensor_type";
    private ConnectionWrapper mConnectionWrapper;
    private static MyApplication self;
    private SharedPreferences sharedPreferences;


    public static synchronized MyApplication getInstance() {
        return self;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (MyApplication.class) {
            self = this;
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    public void createConnectionWrapper(ConnectionWrapper.OnCreatedListener listener) {
        mConnectionWrapper = new ConnectionWrapper(getApplicationContext(), listener);
    }

    public ConnectionWrapper getConnectionWrapper() {
        return mConnectionWrapper;
    }


    public void setSendetType(int type){
        sharedPreferences.edit().putInt(SENSOR_TYPE, type).commit();
    }


    public int getSendedType(){
        return sharedPreferences.getInt(SENSOR_TYPE, 0);
    }

    public static float round(float number, int scale) {
        int pow = 10;
        for (int i = 1; i < scale; i++)
            pow *= 10;
        float tmp = number * pow;
        return (float) (int) ((tmp - (int) tmp) >= 0.5f ? tmp + 1 : tmp) / pow;
    }
}
