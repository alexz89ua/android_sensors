package com.stfalcon.server;

import android.content.Context;
import com.octo.android.robospice.request.SpiceRequest;

import java.io.*;

/**
 * Created by alexandr on 26.03.14.
 */
public class AnalyticBackgroundProcess extends SpiceRequest<DataLines> {
    private final int ELEMENTS_IN_QWANT = 50;
    private InputStream in;
    private BufferedReader reader;
    private String line;
    private File file;
    private Context context;
    private File templateFile, dataFile;
    private OutputStreamWriter outputStreamT, outputStreamD;
    private FileOutputStream outT,outD;
    private final String tempT = "/sdcard/AccelData/tempT.txt";
    private final String tempD = "/sdcard/AccelData/tempD.txt";


    public AnalyticBackgroundProcess(File data, Context context) {
        super(DataLines.class);
        this.setPriority(PRIORITY_HIGH);
        file = data;
        this.context = context;
        templateFile = new File(tempT);
        dataFile = new File(tempD);
        try {
            templateFile.createNewFile();
            dataFile.createNewFile();

            outT = new FileOutputStream(templateFile);
            outD = new FileOutputStream(dataFile);

            outputStreamT = new OutputStreamWriter(outT);
            outputStreamD = new OutputStreamWriter(outD);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public DataLines loadDataFromNetwork() throws Exception {

       // RoadTemplates roadTemplates = new RoadTemplates(context);

        DataLines dataLines = new DataLines();

        in = new FileInputStream(file);
        reader = new BufferedReader(new InputStreamReader(in));
        line = reader.readLine();

        do {
            dataLines.add(MyApplication.removeTabs(line));
            line = reader.readLine();
        } while (line != null);

/*
        int quantumCount = dataLines.size() / ELEMENTS_IN_QWANT;
        int templateCount = roadTemplates.getTemplatesCount();

        Log.i("Loger", "quantumCount = " + quantumCount);
        Log.i("Loger", "templateCount = " + templateCount);

        for (int templateNum = 0; templateNum < templateCount; templateNum++) {

            for (int quantum = 1; quantum < quantumCount; quantum++) {

                outputStreamT = new OutputStreamWriter(outT);
                outputStreamD = new OutputStreamWriter(outD);

                String compairLine = getQwantLine(dataLines, quantum);
                String templateLine = getTemplateLine(roadTemplates.getTemplate(templateNum));

                outputStreamD.write(compairLine);
                outputStreamT.write(templateLine);

                outputStreamD.close();
                outputStreamT.close();

                String[] args = {tempD, tempT, "20"};
                Log.i("Loger", "compairLine = " + compairLine);
                Log.i("Loger", "templateLine = " + templateLine);
                Log.i("Loger", "args = " + args.length);
                FastDtwTest.main(args);
            }

        }

        roadTemplates.closeTemplate();*/
        return dataLines;
    }


    private String getQwantLine(DataLines dataLines, int quantum){
        String compairLine = "";
        for (int i = quantum; i < quantum * ELEMENTS_IN_QWANT; i++) {
            String[] arr = dataLines.get(i).split("\t", 9);
            Float pit = Float.valueOf(arr[4]);
            String point = i + "," + pit + "\n";
            compairLine = compairLine + point;
        }
        return compairLine;
    }

    private String getTemplateLine(DataLines template){
        String templateLine = "";
        int linesCount = template.size();
        for (int i = 0; i < linesCount; i++) {
            String[] arr = template.get(i).split("\t", 9);
            Float pit = Float.valueOf(arr[4]);
            String point = i + "," + pit + "\n";
            templateLine = templateLine + point;
        }
        return templateLine;
    }

}
