package com.stfalcon.server;

import android.content.Context;
import android.util.Log;
import com.octo.android.robospice.request.SpiceRequest;

import java.io.*;
import java.util.List;

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


    public AnalyticBackgroundProcess(File data, Context context) {
        super(DataLines.class);
        file = data;
        this.context = context;
    }

    @Override
    public DataLines loadDataFromNetwork() throws Exception {

        Log.i("Loger", "loadDataFromNetwork");

        //RoadTemplates roadTemplates = new RoadTemplates(context);

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

                boolean valid = validating(getQwantLine(dataLines, quantum), getTemplateLine(roadTemplates.getTemplate(templateNum)));

                Log.i("Loger", "valid = " + valid);

            }

        }

        roadTemplates.closeTemplate();*/
        return dataLines;
    }


    private double[] getQwantLine(DataLines dataLines, int quantum) {
        int start = quantum;
        double[] compairLine = new double[dataLines.size()];
        for (int i = quantum; i < quantum * ELEMENTS_IN_QWANT; i++) {
            String[] arr = dataLines.get(i).split("\t", 9);
            if (arr.length > 4) {
                Float pit = Float.valueOf(arr[4]);
                compairLine[quantum - start] = pit;
            }
        }
        return compairLine;
    }

    private double[] getTemplateLine(DataLines template) {
        double[] templateLine = new double[template.size()];
        int linesCount = template.size();
        for (int i = 0; i < linesCount; i++) {
            String[] arr = template.get(i).split("\t", 9);
            if (arr.length > 4) {
                Float pit = Float.valueOf(arr[4]);
                templateLine[i] = pit;
            }
        }
        return templateLine;
    }


    /**
     * Сравнивает записанные жесты
     */
    public boolean validating(double[] pArr, double[] rArr) {
        boolean isValid = false;

        List<double[]> pList = Comparison.prepareArrays(pArr, rArr);

        if (pList == null) {
            return false;
        }

        pArr = pList.get(0);
        rArr = pList.get(1);

        double[] x = pArr;
        double[] x1 = rArr;

        double PirsonKoef = Comparison.pirsonCompare(x, x1);
        Log.i("Loger", "PirsonKoef = " + PirsonKoef);
        FACTOR factor = getFactors();
        boolean сompare = (PirsonKoef >= factor.getFactor());
        if (сompare) {
            isValid = true;
        }

        return isValid;
    }


    private FACTOR getFactors() {
        return new FACTOR(0.1);
    }

    private class FACTOR {
        final double factor;

        public FACTOR(double factor) {
            this.factor = factor;
        }

        public double getFactor() {
            return factor;
        }
    }

}
