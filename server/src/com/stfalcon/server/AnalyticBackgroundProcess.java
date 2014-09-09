package com.stfalcon.server;

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

        while (line != null){
            line = reader.readLine();
                dataLines.add(line);
        }
        in.close();






        return dataLines;
    }



}
