package com.stfalcon.server.service;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.stfalcon.server.MyApplication;
import com.stfalcon.server.connection.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.HashMap;

/**
 * Created by alexandr on 19.08.14.
 */
public class WriteService extends Service {
    public WriteBinder binder = new WriteBinder();
    private HashMap<String, OutputStreamWriter> outputStream = new HashMap<String, OutputStreamWriter>();


    private boolean createdConnectionWrapper = false;

    @Override
    public void onCreate() {
        super.onCreate();

        MyApplication.getInstance().createConnectionWrapper(
                new ConnectionWrapper.OnCreatedListener() {
                    @Override
                    public void onCreated() {
                        createdConnectionWrapper = true;
                        Intent intentTracking = new Intent(MyApplication.MESSAGE);
                        intentTracking.putExtra(MyApplication.CREATED, true);
                        LocalBroadcastManager.getInstance(WriteService.this).sendBroadcast(intentTracking);
                    }
                }
        );
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    public class WriteBinder extends Binder {
        public WriteService getService() {
            return WriteService.this;
        }
    }


    // WIFI CONNECTION

    /**
     *
     */
    public void startServer() {
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        int intaddr = wifi.getConnectionInfo().getIpAddress();

        if (wifi.getWifiState() == WifiManager.WIFI_STATE_DISABLED || intaddr == 0) {
            Intent intentTracking = new Intent(MyApplication.MESSAGE);
            intentTracking.putExtra(MyApplication.WIFI, true);
            LocalBroadcastManager.getInstance(WriteService.this).sendBroadcast(intentTracking);
        } else {
            getConnectionWrapper().stopNetworkDiscovery();
            getConnectionWrapper().startServer();
            getConnectionWrapper().setHandler(mServerHandler);

            Intent intentTracking = new Intent(MyApplication.MESSAGE);
            intentTracking.putExtra(MyApplication.STARTED, true);
            LocalBroadcastManager.getInstance(WriteService.this).sendBroadcast(intentTracking);
        }
    }




    /**
     *
     * @param device
     */
    public void createFileToWrite(String device){

        try {

            File directory = new File("/sdcard/DCIM/UARoads/");
            directory.mkdirs();

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String time = simpleDateFormat.format(System.currentTimeMillis());

            File myFile = new File("/sdcard/DCIM/UARoads/" + time + " " + device + ".txt");
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);

            outputStream.put(device, new OutputStreamWriter(fOut));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    /**
     *
     * @param device
     */
    public void createFileToWriteResults(String device){

        try {

            File directory = new File("/sdcard/DCIM/UARoads/Results/");
            directory.mkdirs();

            File myFile = new File("/sdcard/DCIM/UARoads/Results/" + device + "RESULT" +  ".txt");
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);

            outputStream.put(device, new OutputStreamWriter(fOut));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    /**
     *
     * @param device
     * @param data
     */
    public void writeToFile(String device, String data){
        try {
            outputStream.get(device).write(data);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception i){
            i.printStackTrace();
        }
    }


    /**
     *
     */
    public void stopWriteToFile(){
       for (String device : outputStream.keySet()){
           try {
               outputStream.get(device).close();
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
    }


    /**
     *
     */
    public void connect() {
        try{
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
        } catch (Exception e){
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

    /**
     *
     */
    private Handler mServerHandler = new MessageHandler() {
        @Override
        public void onMessage(String type, JSONObject message) {
            try {


                if (type.equals(Communication.Connect.DATA)) {

                    //Log.d("Loger", "mServerHandler have data");
                    final String deviceFrom = message.getString(Communication.Connect.DEVICE);
                    final String data = message.getString(MyApplication.SENSOR);

                    Intent intentTracking = new Intent(MyApplication.MESSAGE);
                    intentTracking.putExtra(MyApplication.DEVICE, "Device: " + deviceFrom);
                    intentTracking.putExtra(MyApplication.SENSOR, data);
                    LocalBroadcastManager.getInstance(WriteService.this).sendBroadcast(intentTracking);

                }


                if (type.equals(Communication.Connect.DEVICE)) {
                    final String deviceFrom = message.getString(Communication.Connect.DEVICE);

                    Intent intentTracking = new Intent(MyApplication.MESSAGE);
                    intentTracking.putExtra(MyApplication.DEVICE, "Device: " + deviceFrom);
                    LocalBroadcastManager.getInstance(WriteService.this).sendBroadcast(intentTracking);

                    getConnectionWrapper().send(
                            new HashMap<String, String>() {{
                                put(Communication.MESSAGE_TYPE, Communication.Connect.SUCCESS);
                            }}
                    );
                }

                if (type.equals(Communication.Connect.SUCCESS)) {

                    Intent intentTracking = new Intent(MyApplication.MESSAGE);
                    intentTracking.putExtra(MyApplication.DEVICE, "connect");
                    LocalBroadcastManager.getInstance(WriteService.this).sendBroadcast(intentTracking);

                }


            } catch (JSONException e) {
                Log.d("Loger", "JSON parsing exception: " + e);
            }
        }
    };

    private Handler mClientHandler = new MessageHandler() {
        @Override
        public void onMessage(String type, JSONObject message) {
            if (type.equals(Communication.ConnectSuccess.TYPE)) {
                Intent intentTracking = new Intent(MyApplication.MESSAGE);
                LocalBroadcastManager.getInstance(WriteService.this).sendBroadcast(intentTracking);
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

