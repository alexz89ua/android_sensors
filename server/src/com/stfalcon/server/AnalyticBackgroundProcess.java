package com.stfalcon.server;

import android.content.Context;
import android.util.Log;
import com.octo.android.robospice.request.SpiceRequest;
import com.stfalcon.server.entity.DataLines;
import com.stfalcon.server.entity.Pit;
import com.stfalcon.server.entity.Pits;

import java.io.*;

/**
 * Created by alexandr on 26.03.14.
 */
public class AnalyticBackgroundProcess extends SpiceRequest<Pits> {
    private final int ELEMENTS_IN_QWANT = 50;
    private InputStream in;
    private BufferedReader reader;
    private String line;
    private File file;
    private Context context;
    private Pits pits = new Pits();


    public AnalyticBackgroundProcess(File data, Context context) {
        super(Pits.class);
        file = data;
        this.context = context;
    }

    @Override
    public Pits loadDataFromNetwork() throws Exception {

        Log.i("Loger", "loadDataFromNetwork");

        DataLines dataLines = new DataLines();

        in = new FileInputStream(file);
        reader = new BufferedReader(new InputStreamReader(in));
        line = reader.readLine();

        do {
            dataLines.add(MyApplication.removeTabs(line));
            line = reader.readLine();
        } while (line != null);

        int linesCount = dataLines.size();
        for (int i = 1; i < linesCount; i++) {
            String[] arr = dataLines.get(i).split("\t", 9);
            if (arr.length > 4) {
                Pit pit = new Pit();
                pit.acc = Double.valueOf(arr[2]);

                pit.lat = Double.valueOf(arr[5]);
                pit.lon = Double.valueOf(arr[6]);
                pit.speed = Double.valueOf(arr[7]);
                pit.distance = pit.speed/3.6 * i / 36;
                //pit.distance = pit.speed * i / 36;
                pit.sizeH = (Math.abs(pit.acc) * 1000) / Math.pow(pit.speed, 1.58);
                if (pit.acc < 0) {
                    pits.add(pit);
                }

            }
        }


        return pits;
    }

}
