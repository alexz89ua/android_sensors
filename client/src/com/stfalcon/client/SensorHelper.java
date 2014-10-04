package com.stfalcon.client;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;

/**
 * Created by alexandr on 03.09.14.
 */
public class SensorHelper {

    public static final int SENSOR_DELAY_36Hz = 27777; // in  microseconds

    public static final int TYPE_A = 0;   // ACCELEROMETER
    public static final int TYPE_L = 1;   // LINEAR_ACCELERATION
    public static final int TYPE_G = 2;   // GRAVITY

    private static float[] motion = new float[3];
    private static float[] gravity = new float[3];


    private void getSensorDelay(SensorManager sensorManager) {
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Log.i("Loger", "TYPE_ACCELEROMETER - " + sensor.getName());
        Log.i("Loger", "Version - " + sensor.getVersion());
        Log.i("Loger", "Type - " + sensor.getType());
        Log.i("Loger", "Vendor - " + sensor.getVendor());
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Log.i("Loger", "FifoMaxEventCount - " + sensor.getFifoMaxEventCount());
            Log.i("Loger", "FifoReservedEventCount - " + sensor.getFifoReservedEventCount());
        }
        Log.i("Loger", "MaximumRange - " + sensor.getMaximumRange());
        Log.i("Loger", "MinDelay - " + sensor.getMinDelay());
        Log.i("Loger", "Power - " + sensor.getPower());
        Log.i("Loger", "Resolution - " + sensor.getResolution());

    }


    /**
     * Підписуємось на прослуховування датчиків
     *
     * @param listener
     * @param sensorManager
     * @param sensorType
     */
    public static void registrateListener(SensorEventListener listener, SensorManager sensorManager, int sensorType) {
        switch (sensorType) {
            case TYPE_L:
                sensorManager.registerListener(listener, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SENSOR_DELAY_36Hz);
                break;

            case TYPE_G:
                sensorManager.registerListener(listener, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SENSOR_DELAY_36Hz);
                break;

            default:
                sensorManager.registerListener(listener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SENSOR_DELAY_36Hz);
                break;
        }
    }


    /**
     * Аналізує данні що надійшли з сенсорів
     *
     * @param sensorEvent
     * @param time
     * @return Object[] result  - де  long   result[0] - поточний час надходження,
     * String result[1] - оброблені дані сенсора,
     * int    result[2] - тип сенсора
     */
    public static Object[] analyzeSensorEvent(SensorEvent sensorEvent, long time) {

        Object[] result = new Object[3];

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {


            float x = MyApplication.round(sensorEvent.values[0], 2);
            float y = MyApplication.round(sensorEvent.values[1], 2);
            float z = MyApplication.round(sensorEvent.values[2], 2);

            String dataA = time + " " + x + " " + y + " " + z;
            //Log.i("Loger", dataA);

            result[0] = System.currentTimeMillis();
            result[1] = dataA;
            result[2] = SensorHelper.TYPE_A;

            return result;


        }

        if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

            float x = MyApplication.round(sensorEvent.values[0], 2);
            float y = MyApplication.round(sensorEvent.values[1], 2);
            float z = MyApplication.round(sensorEvent.values[2], 2);

            String dataL = time + " " + x + " " + y + " " + z;

            result[0] = System.currentTimeMillis();
            result[1] = dataL;
            result[2] = SensorHelper.TYPE_L;

            return result;

        }


        if (sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {

            for (int i = 0; i < 3; i++) {
                gravity[i] = (float) (0.1 * sensorEvent.values[i] + 0.9 * gravity[i]);
                motion[i] = sensorEvent.values[i] - gravity[i];
            }

            float x = MyApplication.round(motion[0], 2);
            float y = MyApplication.round(motion[1], 2);
            float z = MyApplication.round(motion[2], 2);

            String dataG = time + " " + x + " " + y + " " + z;

            result[0] = System.currentTimeMillis();
            result[1] = dataG;
            result[2] = SensorHelper.TYPE_G;

            return result;
        }
        return null;
    }


}
