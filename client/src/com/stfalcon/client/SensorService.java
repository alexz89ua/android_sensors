package com.stfalcon.client;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.stfalcon.client.connection.*;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by alexandr on 19.08.14.
 */
public class SensorService extends Service implements SensorEventListener {
    private static final long SENDING_DATA_INTERVAL_IN_MILLIS = 1000;
    private static final float MAXIMUM_POSSIBLE_ACCELERATION = 2.5f;

    private int NOTIFICATION = 1000;
    public WriteBinder binder = new WriteBinder();
    private int activeSensorType;

    private SensorManager sensorManager;
    public LocationManager locationManager;
    public MyLocationListener listener;
    public Location previousBestLocation = new Location("gps");
    private double lastBestSpeed = 0.0f;
    private long lastSpeedTime = 0;
    private long startListeningTime = 0;
    private boolean createdConnectionWrapper = false;

    private List<String> dataToSend = new ArrayList<String>();
    private long lastSendingTime = 0l;
    private long lastGettingTime = 0l;
    private int counter;


    //private TookThePhone tookThePhone;


    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

    //    tookThePhone = new TookThePhone();

        // @todo ?
        MyApplication.getInstance().createConnectionWrapper(
                new ConnectionWrapper.OnCreatedListener() {
                    @Override
                    public void onCreated() {
                        createdConnectionWrapper = true;
                    }
                }
        );
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    /**
     * Викликається при натиску на кнопку Старт. Створюється лісенер на локацію і на сенсор
     * Додає нотифікацію в статус бар
     */
    public void startListening() {
        activeSensorType = MyApplication.getInstance().getSendedType();
        startForeground(NOTIFICATION, makeNotification());
        Log.v("Loger", "START_DONE");


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);

    //    tookThePhone.registrateListener(this, sensorManager);
        SensorHelper.registrateListener(this, sensorManager, activeSensorType);

        startListeningTime = System.currentTimeMillis();
    }


    /**
     * Викликається при натисканні на стоп. Вилучає лісенери і видаляє нотифікацію з статус бару
     */
    public void stopListening() {
        Log.v("Loger", "STOP_DONE");

        sensorManager.unregisterListener(this);

    //    tookThePhone.stopWriteToFile();

        stopForeground(true);

        if (listener != null) {
            locationManager.removeUpdates(listener);
        }
    }


    /**
     * Додає до данних координати і айді девайсу. Після чого формує пакети і періодично передає на сервер
     */
    public void sendNewData(Object[] resultData) {

        final long time = (Long) resultData[0];
        String data = (String) resultData[1];
        final int type = (Integer) resultData[2];

        if (createdConnectionWrapper) {
            if (type == activeSensorType && data != null) {
                String loc = " " + previousBestLocation.getLatitude() + " " + previousBestLocation.getLongitude();
                data = data + loc + " " + String.valueOf(validateSpeed(0)) + "\n";

                final String stringDataToSend = data;

                getConnectionWrapper().send(
                        new HashMap<String, String>() {{
                            put(Communication.MESSAGE_TYPE, Communication.Connect.DATA);
                            put(Communication.Connect.DEVICE, createDeviceDescription(type));
                            put(MyApplication.SENSOR, stringDataToSend);
                        }}
                );

                lastSendingTime = time;
            }
        }
    }

    /**
     * Формує айдішку девайсу. Використовується для підпису на графіку і в файлі
     */
    private String createDeviceDescription(int type) {
        String stringType = "";
        switch (type) {
            case SensorHelper.TYPE_A:
                stringType = "Accel";
                break;
            case SensorHelper.TYPE_G:
                stringType = "Gravity";
                break;
            case SensorHelper.TYPE_L:
                stringType = "Linear-Accel";
                break;
        }

        String serial = Build.SERIAL;
        serial = serial.substring(serial.length() - 3);
        return Build.MODEL + "+" + serial + "-" + stringType;
    }

    /**
     * Створює нотифікацію в статус барі
     */
    public Notification makeNotification() {

        Intent intent = new Intent(this, MyActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.stat_sys_upload)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.send))
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setContentIntent(contentIntent);

        mBuilder.setAutoCancel(true);
        return mBuilder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    /**
     * Рахує прискорення та визначає чи є воно адекватним
     *
     * @param speed перевірену на адекватність
     * @return
     */
    public synchronized double validateSpeed(double speed) {
        if (speed != 0) {
            double dV = Math.abs(speed - lastBestSpeed);
            long dT = (System.currentTimeMillis() - lastSpeedTime) / 1000;
            double a = dV / dT;
            Log.v("Loger", "dV=" + dV);
            Log.v("Loger", "dT=" + dT);
            Log.v("Loger", "a=" + a);
            if (Math.abs(a) < MAXIMUM_POSSIBLE_ACCELERATION) {
                lastBestSpeed = speed;
            } else {
                Log.v("Loger", "false");
            }
            lastSpeedTime = System.currentTimeMillis();
        }
        return lastBestSpeed * 3.6;
    }
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (lastSendingTime == 0) {
            lastSendingTime = System.currentTimeMillis();
            counter = 0;
        }

        long time = System.currentTimeMillis() - lastSendingTime;

        lastGettingTime = System.currentTimeMillis();

        counter++;

        if (lastGettingTime - lastSendingTime > SENDING_DATA_INTERVAL_IN_MILLIS) {
            Log.i("Loger", "COUNT " + counter);
            counter = 0;
        }

        if (sensorEvent.sensor.getType() != Sensor.TYPE_ORIENTATION) {
            sendNewData(SensorHelper.analyzeSensorEvent(sensorEvent, time));
        } else {
   ///         tookThePhone.analyzeSensorEvent(sensorEvent);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * Для передачі даних в актівіті
     */
    public class WriteBinder extends Binder {
        public SensorService getService() {
            return SensorService.this;
        }
    }

    /**
     *
     */
    public class MyLocationListener implements LocationListener {

        public void onLocationChanged(final Location loc) {
            Log.i("Loger", "Location changed, Accuracy = " + loc.getAccuracy());
            previousBestLocation = loc;
            validateSpeed(loc.getSpeed());
        }

        public void onProviderDisabled(String provider) {
        }


        public void onProviderEnabled(String provider) {
        }


        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

    }

    /**
     * Обробляє натискання на кнопку Коннект
     */
    public void connect() {
        try {
            if (createdConnectionWrapper) {
                getConnectionWrapper().findServers(new NetworkDiscovery.OnFoundListener() {
                    @Override
                    public void onFound(javax.jmdns.ServiceInfo info) {
                        if (info != null && info.getInet4Addresses().length > 0) {
                            getConnectionWrapper().stopNetworkDiscovery();
                            getConnectionWrapper().connectToServer(
                                    info.getInet4Addresses()[0],
                                    info.getPort(),
                                    mConnectionListener
                            );
                            getConnectionWrapper().setHandler(mClientHandler);
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    private Connection.ConnectionListener mConnectionListener = new Connection.ConnectionListener() {
        @Override
        public void onConnection() {
            getConnectionWrapper().send(
                    new HashMap<String, String>() {{
                        put(Communication.MESSAGE_TYPE, Communication.Connect.DEVICE);
                        put(Communication.Connect.DEVICE, Build.MODEL);
                    }}
            );
        }
    };

    private Handler mClientHandler = new MessageHandler() {
        @Override
        public void onMessage(String type, JSONObject message) {
            if (type.equals(Communication.ConnectSuccess.TYPE)) {
                Intent intentTracking = new Intent(MyApplication.CONNECTED);
                LocalBroadcastManager.getInstance(SensorService.this).sendBroadcast(intentTracking);
                createdConnectionWrapper = true;
            }
        }
    };

    @Override
    public void onDestroy() {
        getConnectionWrapper().reset();
        super.onDestroy();
    }

    private ConnectionWrapper getConnectionWrapper() {
        return MyApplication.getInstance().getConnectionWrapper();
    }
}

