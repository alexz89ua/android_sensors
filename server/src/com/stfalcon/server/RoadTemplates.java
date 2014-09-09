package com.stfalcon.server;

import android.content.Context;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by alexandr on 09.09.14.
 */
public class RoadTemplates {

    private InputStream in;
    private BufferedReader reader;
    private String line;
    private ArrayList<DataLines> templates = new ArrayList<DataLines>();
    private File myFile = new File("/sdcard/AccelData/templates.txt");


    public RoadTemplates(Context context) {
        try {
            DataLines dataLines = new DataLines();
            in = new FileInputStream(myFile);
            reader = new BufferedReader(new InputStreamReader(in));
            line = reader.readLine();

            do {
                dataLines.add(MyApplication.removeTabs(line));
                line = reader.readLine();

                if (line.equals("#")) {
                    templates.add(dataLines);
                    dataLines.clear();
                }

            } while (line != null);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getTemplatesCount() {
        return templates.size();
    }


    public DataLines getTemplate(int i) {
        return templates.get(i);
    }


    public void improove(DataLines dataLines, int templateNum) {
        templates.remove(templateNum);
        templates.add(dataLines);
    }


    public void closeTemplate() {
        try {
            String data = "";

            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter outputStream = new OutputStreamWriter(fOut);

            int count = templates.size();

            for (int i = 0; i < count; i++) {
                int dataLinesCount = templates.get(i).size();
                for (int j = 0; j < dataLinesCount; j++) {
                    outputStream.write(templates.get(i).get(j));
                }
                outputStream.write("#\n");
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
