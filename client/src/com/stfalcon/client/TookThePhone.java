package com.stfalcon.client;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;

/**
 * Created by alexandr on 06/10/14.
 */
public class TookThePhone {

    public static final int SENSOR_DELAY_36Hz = 27777; // in  microseconds
    private float SEARCHENG_ANGLE = 30;

    private final long ONE_SECOND = 1000;
    private long LAST_SECOND = 0;
    private long currentCounter = 0;
    private int angleCount = 0;
    private float lastAngle = 0;

    private OutputStreamWriter outputStream;


    /**
     * Підписуємось на прослуховування датчиків
     *
     * @param listener
     * @param sensorManager
     */
    public void registrateListener(SensorEventListener listener, SensorManager sensorManager) {
        LAST_SECOND = System.currentTimeMillis();
        createFileToWriteResults();
        sensorManager.registerListener(listener, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SENSOR_DELAY_36Hz);
    }


    /**
     * Аналізує данні що надійшли з сенсорів
     *
     * @param sensorEvent
     */
    public void analyzeSensorEvent(SensorEvent sensorEvent) {


        if (sensorEvent.sensor.getType() == Sensor.TYPE_ORIENTATION) {

            long now = System.currentTimeMillis();
            currentCounter++;

            if (now - LAST_SECOND > ONE_SECOND) {
                LAST_SECOND = now;
                angleCount = 0;
            }


            float angle = MyApplication.round(sensorEvent.values[0], 2);

            if (Math.abs(angle) - Math.abs(lastAngle) > SEARCHENG_ANGLE) {
                angleCount++;
            }

            lastAngle = angle;

            String data = currentCounter + " " + angle + " " + angleCount + "\n";

            writeToFile(data);
        }

    }


    /**
     *
     */
    private void createFileToWriteResults() {

        try {

            File directory = new File("/sdcard/DCIM/UARoads/");
            directory.mkdirs();

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String time = simpleDateFormat.format(System.currentTimeMillis());

            File myFile = new File("/sdcard/DCIM/UARoads/" + time + "_TookThePhone.txt");
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);

            outputStream = new OutputStreamWriter(fOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * @param data
     */
    private void writeToFile(String data) {
        try {
            outputStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception i) {
            i.printStackTrace();
        }
    }


    /**
     *
     */
    public void stopWriteToFile() {
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
