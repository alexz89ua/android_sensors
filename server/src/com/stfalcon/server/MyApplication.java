package com.stfalcon.server;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.stfalcon.server.connection.ConnectionWrapper;

/**
 * @author alwx
 * @version 1.0
 */
public class MyApplication extends Application {
    public static final String CONNECTED = "com.example.sasha.CONNECTED";
    public static final String DATA = "com.example.sasha.DATA";
    public static final String DEVICE = "device";
    public static final String WIFI = "wifi";
    public static final String STARTED = "started";
    public static final String SENSOR = "sensor";
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


    public static float round(float number, int scale) {
        int pow = 10;
        for (int i = 1; i < scale; i++)
            pow *= 10;
        float tmp = number * pow;
        return (float) (int) ((tmp - (int) tmp) >= 0.5f ? tmp + 1 : tmp) / pow;
    }


    public static String removeTabs(String line) {

        int charLength = line.length();
        for (int i = 1; i < charLength; i++) {   // start from second char
            if (line.charAt(i) == '\t' && line.charAt(i + 1) == '\t') {
                line = line.substring(0, i) + "" + line.substring(i + 1);
                charLength--;
                i--;
            }
        }
        return line;
    }

}
