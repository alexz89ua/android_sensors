package com.stfalcon.server;

import android.content.Context;
import android.util.Log;
import com.octo.android.robospice.request.SpiceRequest;
import com.stfalcon.server.entity.DataLines;
import com.stfalcon.server.entity.Pit;
import com.stfalcon.server.entity.Pits;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by alexandr on 26.03.14.
 */
public class AnalyticBackgroundProcess extends SpiceRequest<Pits> {
    private int ELEMENTS_IN_MINUTE = 2000;
    private InputStream in;
    private BufferedReader reader;
    private String line;
    private File file;
    private Context context;
    private Pits pits = new Pits();

    private ArrayList<Double> X_MAX_ELEMENTS_COUNT = new ArrayList<Double>();
    private ArrayList<Double> Y_MAX_ELEMENTS_COUNT = new ArrayList<Double>();
    private ArrayList<Double> Z_MAX_ELEMENTS_COUNT = new ArrayList<Double>();


    public AnalyticBackgroundProcess(File data, Context context) {
        super(Pits.class);
        file = data;
        this.context = context;
    }

    @Override
    public Pits loadDataFromNetwork() throws Exception {

        int targetValues = 1;
        ArrayList<Double> values = new ArrayList<Double>();
        DataLines dataLines = new DataLines();

        try {


            // зчитуємо вхідні дані з файлу
            in = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(in));
            line = reader.readLine();

            do {
                dataLines.add(MyApplication.removeTabs(line));
                line = reader.readLine();
            } while (line != null);


            // визначаємось з кількістю рядків для аналізу осей,
            // щоб у подальшому визначити потрібну для обрахунків
            int linesCount = dataLines.size();
            if (linesCount < ELEMENTS_IN_MINUTE) {
                ELEMENTS_IN_MINUTE = linesCount;
            }


            // знаходимо число максимальних значень по кожній з осей
            for (int i = 1; i < ELEMENTS_IN_MINUTE; i++) {
                String[] arr = dataLines.get(i).split("\t", 9);

                double x = Math.abs(Double.valueOf(arr[1]));
                double y = Math.abs(Double.valueOf(arr[2]));
                double z = Math.abs(Double.valueOf(arr[3]));

                values.add(x);
                values.add(y);
                values.add(z);

                double max = Collections.max(values);

                values.clear();

                if (max == x) {
                    X_MAX_ELEMENTS_COUNT.add(x);
                }

                if (max == y) {
                    Y_MAX_ELEMENTS_COUNT.add(y);
                }

                if (max == z) {
                    Z_MAX_ELEMENTS_COUNT.add(z);
                }
            }


            // визначаємо вісь з найбільшою кількістю максимумів
            if (X_MAX_ELEMENTS_COUNT.size() >= Y_MAX_ELEMENTS_COUNT.size() &&
                    X_MAX_ELEMENTS_COUNT.size() >= Z_MAX_ELEMENTS_COUNT.size()) {
                targetValues = 1;
                Log.i("Loger", "targetValues X");
            } else if (Y_MAX_ELEMENTS_COUNT.size() >= X_MAX_ELEMENTS_COUNT.size() &&
                    Y_MAX_ELEMENTS_COUNT.size() >= Z_MAX_ELEMENTS_COUNT.size()) {
                targetValues = 2;
                Log.i("Loger", "targetValues Y");
            } else if (Z_MAX_ELEMENTS_COUNT.size() >= X_MAX_ELEMENTS_COUNT.size() &&
                    Z_MAX_ELEMENTS_COUNT.size() >= Y_MAX_ELEMENTS_COUNT.size()) {
                targetValues = 3;
                Log.i("Loger", "targetValues Z");
            }
            Log.i("Loger", "X_MAX_ELEMENTS_COUNT = " + X_MAX_ELEMENTS_COUNT.size() + " " +
                    "Y_MAX_ELEMENTS_COUNT = " + Y_MAX_ELEMENTS_COUNT.size() + " " +
                    "Z_MAX_ELEMENTS_COUNT = " + Z_MAX_ELEMENTS_COUNT.size());



            // розраховуємо параметри нерівностей згідно обраної осі
            for (int i = 1; i < linesCount; i++) {
                String[] arr = dataLines.get(i).split("\t", 9);

                if (arr.length > 4) {
                    Pit pit = new Pit();

                    pit.acc = Math.abs(Double.valueOf(arr[targetValues]));
                    pit.lat = Double.valueOf(arr[5]);
                    pit.lon = Double.valueOf(arr[6]);
                    pit.speed = MyApplication.round(Double.valueOf(arr[7]), 2);
                    pit.distance = pit.speed / 3.6 * i / 36;
                    pit.sizeH = MyApplication.round((Math.abs(pit.acc) * 100) / Math.pow(pit.speed, 1.58), 2);
                    pits.add(pit);

                }
            }


        } catch (Exception e) {
            e.printStackTrace();
            Log.i("Loger", "Exception in analytic");

        }

        return pits;
    }

}
