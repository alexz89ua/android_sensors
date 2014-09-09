package com.stfalcon.server;

import android.util.Log;
import com.octo.android.robospice.request.SpiceRequest;

import java.io.*;

/**
 * Created by alexandr on 26.03.14.
 */
public class AnalyticBackgroundProcess extends SpiceRequest<DataLines> {
    private InputStream in;
    private BufferedReader reader;
    private String line;
    private File file;


    public AnalyticBackgroundProcess(File data) {
        super(DataLines.class);
        this.setPriority(PRIORITY_HIGH);
        file = data;
    }

    @Override
    public DataLines loadDataFromNetwork() throws Exception {

        DataLines dataLines = new DataLines();

        in = new FileInputStream(file);
        reader = new BufferedReader(new InputStreamReader(in));
        line = reader.readLine();

        do {
            dataLines.add(MyApplication.removeTabs(line));
            line = reader.readLine();
        } while (line != null);

        /*Log.i("Loger", "Line =" + dataLines.get(1));
        in.close();

        String[] arr = dataLines.get(1).split("\t", 9);

        Log.i("Loger", "time = " + arr[0]);
        Log.i("Loger", "x = " + arr[1]);
        Log.i("Loger", "y = " + arr[2]);
        Log.i("Loger", "z = " + arr[3]);
        Log.i("Loger", "sqr = " + arr[4]);
        Log.i("Loger", "lat = " + arr[5]);
        Log.i("Loger", "lon = " + arr[6]);
        Log.i("Loger", "speed = " + arr[7]);
        Log.i("Loger", "color = " + arr[8]);*/




        return dataLines;
    }


}
