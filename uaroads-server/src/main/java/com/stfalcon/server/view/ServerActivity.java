package com.stfalcon.server.view;

import android.content.*;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.*;

import com.google.gson.Gson;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;
import com.stfalcon.server.*;
import com.stfalcon.server.data.AccelData;
import com.stfalcon.server.data.Pits;
import com.stfalcon.server.service.WriteService;
import com.stfalcon.server.util.SoundManager;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYValueSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.util.MathHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class ServerActivity extends BaseSpiceActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private final static int MIN_VALUES_COUNT_PER_SECOND = 5;
    private final static int MAX_VALUES_COUNT_PER_SECOND = 30;
    private final static int MILLISECONDS_BEFORE_REFRESH_GRAPHS = 30;
    public final static int MIN_DELTA_SPEED = 5;
    private Button server, showConsole, writeToFile, analytic;
    private ProgressBar analyticProgress;
    private ServiceConnection sConn;
    private WriteService writeServise;
    private boolean bound = false, write = false;
    private Intent intentService;
    private BroadcastReceiver mReceiver;
    private TextView textView, tvFilterValue, tvFrequency;
    private LinearLayout llChart;
    private boolean pause = false;
    public TextView tvSpeed;
    public RelativeLayout rlSpeed;
    private LinearLayout llConsole;
    private TextView tvConsole;
    private SoundManager soundManager;
    private View mDecorView;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private ArrayList<String> devicesList = new ArrayList<String>();

    private int filterValuePerSecond = 15, counter = 0; //in seconds

    private CheckBox cbX, cbY, cbZ, cbSqrt, cbH, cbAuto;
    private RadioGroup radioGroup;
    private SeekBar seekBarFrequency, seekBarSensativity;
    private float frequency;
    private float speed = 0f;

    ArrayList<DeviceGraphInformation> devices = new ArrayList<DeviceGraphInformation>();
    private GraphicalView graphicalView;
    private XYMultipleSeriesDataset dataSet = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
    private OnResultTaskListener onResultTaskListener = new OnResultTaskListener();
    private Gson gson = new Gson();

    private ArrayList<Double> values = new ArrayList<Double>();
    private String analizFileName;


    private long currentCounter = 0;
    private ImageView conState;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //full screen
        getActionBar().hide();

        setContentView(R.layout.main_land);
        mDecorView = getWindow().getDecorView();

        soundManager = new SoundManager();

        conState = (ImageView) findViewById(R.id.connect_state);
        server = (Button) findViewById(R.id.server);
        analytic = (Button) findViewById(R.id.analytic);
        showConsole = (Button) findViewById(R.id.show_console);
        writeToFile = (Button) findViewById(R.id.write);
        textView = (TextView) findViewById(R.id.text);
        llChart = (LinearLayout) findViewById(R.id.chart);
        tvSpeed = (TextView) findViewById(R.id.speed);
        rlSpeed = (RelativeLayout) findViewById(R.id.rl_speed);
        llConsole = (LinearLayout) findViewById(R.id.ll_console);
        tvConsole = (TextView) findViewById(R.id.tv_console);
        cbAuto = (CheckBox) findViewById(R.id.cb_auto);
        seekBarSensativity = (SeekBar) findViewById(R.id.seek_bar);
        analyticProgress = (ProgressBar) findViewById(R.id.analytic_progress);

        server.setOnClickListener(this);
        analytic.setOnClickListener(this);
        showConsole.setOnClickListener(this);
        writeToFile.setOnClickListener(this);
        findViewById(R.id.clear).setOnClickListener(this);
        findViewById(R.id.pause).setOnClickListener(this);
        findViewById(R.id.plus).setOnClickListener(this);
        findViewById(R.id.minus).setOnClickListener(this);
        findViewById(R.id.screen_shot).setOnClickListener(this);
        findViewById(R.id.show_console).setOnClickListener(this);


        radioGroup = (RadioGroup) findViewById(R.id.radio_group);

        cbX = (CheckBox) findViewById(R.id.rb_x);
        cbY = (CheckBox) findViewById(R.id.rb_y);
        cbZ = (CheckBox) findViewById(R.id.rb_z);
        cbSqrt = (CheckBox) findViewById(R.id.rb_sqrt);
        cbH = (CheckBox) findViewById(R.id.rb_rotation);

        cbX.setOnCheckedChangeListener(this);
        cbY.setOnCheckedChangeListener(this);
        cbZ.setOnCheckedChangeListener(this);
        cbSqrt.setOnCheckedChangeListener(this);
        cbH.setOnCheckedChangeListener(this);

        cbH.performClick();


        analytic.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                File fileDir = new File("/sdcard/DCIM/UARoads/");
                for (File file : fileDir.listFiles()) {
                    if (!file.isDirectory()) {
                        AnalyticBackgroundProcess analiticBackgroundProcess = new AnalyticBackgroundProcess(file, ServerActivity.this);
                        getSpiceManager().execute(analiticBackgroundProcess, onResultTaskListener);
                    }
                }
                return false;
            }
        });


        tvFrequency = (TextView) findViewById(R.id.tv_frequency);
        tvFilterValue = (TextView) findViewById(R.id.filter_value);

        seekBarFrequency = (SeekBar) findViewById(R.id.seek_bar_frequency);
        seekBarFrequency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                frequency = progress;
                tvFrequency.setText(String.valueOf(frequency));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        frequency = seekBarFrequency.getProgress();
        tvFrequency.setText(String.valueOf(frequency));

        intentService = new Intent(this, WriteService.class);

        //Connect location service
        sConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.d("Loger", "MainActivity onServiceConnected");
                writeServise = ((WriteService.WriteBinder) binder).getService();
                bound = true;
            }

            public void onServiceDisconnected(ComponentName name) {
                Log.d("Loger", "MainActivity onServiceDisconnected");
                bound = false;
            }
        };
        updateFilterValue();
    }

    /**
     * Реалізація FULL SCREEN
     *
     * @param hasFocus
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            if (hasFocus) {
                mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        }
    }


    private void updateFilterValue() {
        tvFilterValue.setText(String.valueOf(filterValuePerSecond));
    }


    @Override
    public void onStart() {
        super.onStart();
        registerReceiver();
        bindService(intentService, sConn, BIND_AUTO_CREATE);
    }


    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        if (!bound) return;
        unbindService(sConn);
        bound = false;
    }


    @Override
    protected void onPause() {
        super.onPause();

    }


    @Override
    protected void onResume() {
        super.onResume();

        if (graphicalView == null) {
            LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
            graphicalView = ChartFactory.getLineChartView(this, getDemoDataSet(),
                    getDemoRenderer());
            layout.addView(graphicalView, new LinearLayout.LayoutParams
                    (LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

            graphicalView.setKeepScreenOn(true);

        } else {
            graphicalView.repaint();
        }
    }


    private void clearGraph() {
        if (graphicalView.isChartDrawn()) {
            renderer.removeAllRenderers();
            dataSet.clear();
            devices.clear();
            graphicalView.repaint();
        }
    }


    @Override
    public void onClick(View view) {
        if (bound) {
            switch (view.getId()) {

                case R.id.server:
                    writeServise.startServer();
                    break;

                case R.id.analytic:
                    OpenFileDialog fileDialog = new OpenFileDialog(this)
                            .setFilter(".*\\.txt")
                            .setFileIcon(getResources().getDrawable(R.drawable.txt))
                            .setFolderIcon(getResources().getDrawable(R.drawable.folder))
                            .setOpenDialogListener(new OpenFileDialog.OpenDialogListener() {
                                @Override
                                public void OnSelectedFile(File file) {
                                    if (analytic.isEnabled()) {
                                        Toast.makeText(getApplicationContext(), file.getName(), Toast.LENGTH_LONG).show();
                                        AnalyticBackgroundProcess analiticBackgroundProcess = new AnalyticBackgroundProcess(file, ServerActivity.this);
                                        getSpiceManager().execute(analiticBackgroundProcess, onResultTaskListener);
                                        analyticProgress.setVisibility(View.VISIBLE);
                                        analytic.setEnabled(false);
                                        analizFileName = file.getName();
                                        Log.i("Loger", "OnSelectedFile " + file.getName());
                                    }
                                }
                            });
                    fileDialog.show();
                    break;

                case R.id.pause:
                    if (pause) {
                        pause = false;
                    } else {
                        pause = true;
                    }

                    ((Button) findViewById(R.id.pause)).setText(pause ? "Start" : "Pause");
                    break;
                case R.id.clear:
                    clearGraph();
                    break;


                case R.id.minus:
                    if (filterValuePerSecond > MIN_VALUES_COUNT_PER_SECOND) {
                        filterValuePerSecond--;
                        updateFilterValue();
                    }
                    break;
                case R.id.plus:
                    if (filterValuePerSecond < MAX_VALUES_COUNT_PER_SECOND) {
                        filterValuePerSecond++;
                        updateFilterValue();
                    }
                    break;

                case R.id.screen_shot:
                    makeScreenShot();
                    break;

                case R.id.show_console:
                    if (llConsole.getVisibility() == View.VISIBLE) {
                        llConsole.setVisibility(View.GONE);
                        showConsole.setText("Show Console");
                    } else {
                        llConsole.setVisibility(View.VISIBLE);
                        showConsole.setText("Hide Console");
                    }
                    break;

                case R.id.write:
                    if (!write) {

                        writeToFile.setText("Stop write");
                        write = true;
                        if (write && bound) {
                            for (String device : devicesList) {
                                writeServise.createFileToWrite(device);
                                String dataToWrite = "time" + "\t\t\t\t\t\t\t\t\t" + "x" + "\t\t\t\t\t" + "y" + "\t\t\t\t" + "z" + "\t\t\t\t" + "sqr" +
                                        "\t\t\t\t\t\t\t" + "lat" + "\t\t\t\t\t\t" + "lon" + "\t\t\t\t\t" + "speed" + "\t\t\t" + "pitColor" + "\n";
                                writeServise.writeToFile(device, dataToWrite);
                            }
                        }
                    } else {
                        writeToFile.setText("Write to file");
                        write = false;
                        currentCounter = 0;
                        if (bound) {
                            writeServise.stopWriteToFile();

                        }
                    }
                    break;
            }
        }
    }


    private void makeScreenShot() {
        Bitmap bitmap = graphicalView.toBitmap();


        try {
            File directory = new File("/sdcard/AccelData/ScreenShots/");
            directory.mkdirs();

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String time = simpleDateFormat.format(System.currentTimeMillis());

            File jpegPictureFile = new File("/sdcard/AccelData/ScreenShots/" + time + "_graph.jpeg");
            jpegPictureFile.createNewFile();
            FileOutputStream pictureOutputStream = new FileOutputStream(jpegPictureFile);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, pictureOutputStream);

            MediaStore.Images.Media.insertImage(getContentResolver(), jpegPictureFile.getPath(), null
                    , "Graph Screen Shot");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Create receiver for cache data from services
     */
    private void registerReceiver() {

        IntentFilter intentFilter = new IntentFilter(MyApplication.MESSAGE);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.hasExtra(MyApplication.WIFI)) {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    return;
                }


                if (intent.hasExtra(MyApplication.SENSOR)) {

                    String device = intent.getStringExtra(MyApplication.DEVICE);


                    DeviceGraphInformation information = findDeviceOnGraph(device);
                    if (information == null) {
                        //add new device
                        information = new DeviceGraphInformation(device);
                        createSeriesAndRendersForNewDevice(information);

                        devices.add(information);
                        devicesList.add(getModel(device));
                    }

                    long currentTime = System.currentTimeMillis();


                    if (pause) {
                        information.xSeries.add(currentTime - 500, MathHelper.NULL_VALUE);
                        information.ySeries.add(currentTime - 500, MathHelper.NULL_VALUE);
                        information.zSeries.add(currentTime - 500, MathHelper.NULL_VALUE);
                        information.sqrSeries.add(currentTime - 500, MathHelper.NULL_VALUE);
                        return;
                    }


                    String data = intent.getStringExtra(MyApplication.SENSOR);

                    AccelData accelData = gson.fromJson(data, AccelData.class);

                    String consolText = tvConsole.getText().toString();
                    if (consolText.length() > 800) {
                        consolText = "";
                    }
                    tvConsole.setText(consolText + data);


                    //long sendingTime;
                    //sendingTime = currentTime - accelData.time;
                    //long graphTime = sendingTime + (28);

                    showSpeed(getModel(device), accelData.speed);


                        float pit;

                        switch (radioGroup.getCheckedRadioButtonId()) {
                            case R.id.rb_x:
                                pit = accelData.x;
                                break;
                            case R.id.rb_y:
                                pit = accelData.y;
                                break;
                            case R.id.rb_z:
                                pit = accelData.z;
                                break;
                            case R.id.rb_rotation:
                                pit = accelData.verticalAcc;
                                break;
                            case R.id.rb_sqrt:
                                pit = accelData.sqrt;
                            default:
                                pit = 0;
                                break;

                        }


                        String pitColor = "x";

                        if (write && bound) {

                            String time = String.valueOf(System.currentTimeMillis());
                            String dataToWrite = currentCounter + "\t\t\t" + accelData.x + "\t\t\t" + accelData.y + "\t\t\t" + accelData.z + "\t\t\t" + accelData.sqrt +
                                    "\t\t\t" + accelData.lat + "\t\t\t" + accelData.lon + "\t\t\t" + speed + "\t\t\t" + pitColor +  "\t\t\t" + time + "\n";
                            writeServise.writeToFile(getModel(device), dataToWrite);
                            currentCounter++;
                        }

                  /* // if (information.xSeries.getMaxX() + 1000 < graphTime) {
                        information.xSeries.add(currentTime - 500, MathHelper.NULL_VALUE);
                        information.ySeries.add(currentTime - 500, MathHelper.NULL_VALUE);
                        information.zSeries.add(currentTime - 500, MathHelper.NULL_VALUE);
                        information.sqrSeries.add(currentTime - 500, MathHelper.NULL_VALUE);
                        information.HSeries.add(currentTime - 500, MathHelper.NULL_VALUE);
                  //  }*/

                    if (cbX.isChecked()) {
                        information.xSeries.add(accelData.time, accelData.x);
                    }
                    if (cbY.isChecked()) {
                        information.ySeries.add(accelData.time, accelData.y);
                    }
                    if (cbZ.isChecked()) {
                        information.zSeries.add(accelData.time, accelData.z);
                    }
                    if (cbSqrt.isChecked()) {
                        information.sqrSeries.add(accelData.time, accelData.sqrt);
                    }
                    if (cbH.isChecked()) {
                        information.HSeries.add(accelData.time, accelData.verticalAcc);
                    }


                    renderer.setXAxisMin(System.currentTimeMillis() - 10000);
                    renderer.setXAxisMax(System.currentTimeMillis() + 500);
                    graphicalView.repaint();

                    return;
                }


                if (intent.hasExtra(MyApplication.STARTED)) {
                    textView.setText("Start server...");
                    return;
                }
                if (intent.hasExtra(MyApplication.CREATED)) {
                    conState.setImageResource(android.R.drawable.star_big_on);
                    textView.setText("Server created");
                    return;
                }

                if (intent.hasExtra(MyApplication.DEVICE)) {

                    Toast.makeText(ServerActivity.this,
                            intent.getStringExtra(MyApplication.DEVICE),
                            Toast.LENGTH_LONG).show();
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, intentFilter);
    }


    private void showSpeed(String device, double speed) {
        if (device.equals(devicesList.get(0))) {
            tvSpeed.setText(String.valueOf(speed / 3.6));
            this.speed = (float) speed;
        }
    }


    private DeviceGraphInformation findDeviceOnGraph(String device) {
        for (DeviceGraphInformation information : devices)
            if (information.device.equals(device))
                return information;

        return null;
    }


    private XYMultipleSeriesRenderer getDemoRenderer() {
        renderer = new XYMultipleSeriesRenderer();
        renderer.setAxisTitleTextSize(16);
        renderer.setChartTitleTextSize(20);
        renderer.setLabelsTextSize(15);
        renderer.setLegendTextSize(15);
        renderer.setPointSize(2f);
        renderer.setMargins(new int[]{20, 30, 15, 0});

        renderer.setZoomButtonsVisible(false);
        renderer.setAntialiasing(true);

        renderer.setXAxisMin(0);
        renderer.setXAxisMax(15);
        renderer.setYAxisMin(-5);
        renderer.setYAxisMax(20);

        renderer.setAxesColor(Color.GREEN);
        renderer.setLabelsColor(Color.BLACK);

        renderer.setXLabelsColor(Color.GREEN);
        renderer.setShowGridX(true);
        renderer.setGridColor(Color.GREEN);

        return renderer;
    }


    private XYMultipleSeriesDataset getDemoDataSet() {
        dataSet = new XYMultipleSeriesDataset();
        return dataSet;
    }


    private void createSeriesAndRendersForNewDevice(DeviceGraphInformation information) {
        try {
            createAndAddXSeriesAndRenderer(information);
            createAndAddYSeriesAndRenderer(information);
            createAndAddZSeriesAndRenderer(information);
            createAndAddSqrSeriesAndRenderer(information);
            createAndAddLFFSeriesAndRenderer(information);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void createAndAddSqrSeriesAndRenderer(DeviceGraphInformation information) {
        XYSeriesRenderer r = new XYSeriesRenderer();
        r.setColor(getRandomColor());
        r.setPointStyle(PointStyle.CIRCLE);
        r.setFillPoints(true);
        r.setLineWidth(3f);

        information.sqrSeriesRenderer = r;


        XYValueSeries sqrSeries = new XYValueSeries(information.device + "-sqr");

        information.sqrSeries = sqrSeries;
        if (cbSqrt.isChecked()) {
            renderer.addSeriesRenderer(information.sqrSeriesRenderer);
            dataSet.addSeries(devices.size(), sqrSeries);
        }
    }


    private void createAndAddZSeriesAndRenderer(DeviceGraphInformation information) {
        XYSeriesRenderer r = new XYSeriesRenderer();
        r.setColor(getRandomColor());
        r.setPointStyle(PointStyle.DIAMOND);
        r.setFillPoints(true);
        r.setLineWidth(3f);

        information.zSeriesRenderer = r;

        XYValueSeries zSeries = new XYValueSeries(information.device + "-Z");
        information.zSeries = zSeries;

        if (cbZ.isChecked()) {
            renderer.addSeriesRenderer(information.zSeriesRenderer);
            dataSet.addSeries(devices.size(), zSeries);
        }
    }


    private void createAndAddYSeriesAndRenderer(DeviceGraphInformation information) {
        XYSeriesRenderer r = new XYSeriesRenderer();
        r.setPointStyle(PointStyle.TRIANGLE);
        r.setFillPoints(true);
        r.setColor(getRandomColor());
        r.setLineWidth(3f);

        information.ySeriesRenderer = r;

        XYValueSeries ySeries = new XYValueSeries(information.device + "-Y");
        information.ySeries = ySeries;
        if (cbY.isChecked()) {
            renderer.addSeriesRenderer(information.ySeriesRenderer);
            dataSet.addSeries(devices.size(), ySeries);
        }
    }

    private void createAndAddXSeriesAndRenderer(DeviceGraphInformation information) {
        XYSeriesRenderer r = new XYSeriesRenderer();
        r.setColor(getRandomColor());
        r.setPointStyle(PointStyle.SQUARE);
        r.setFillPoints(true);
        r.setLineWidth(3f);

        information.xSeriesRenderer = r;

        XYValueSeries xSeries = new XYValueSeries(information.device + "-X");
        information.xSeries = xSeries;

        if (cbX.isChecked()) {
            renderer.addSeriesRenderer(information.xSeriesRenderer);
            dataSet.addSeries(devices.size(), xSeries);
        }
    }

    private void createAndAddLFFSeriesAndRenderer(DeviceGraphInformation information) {
        XYSeriesRenderer r = new XYSeriesRenderer();
        r.setColor(getRandomColor());
        r.setPointStyle(PointStyle.POINT);
        r.setFillPoints(true);
        r.setLineWidth(3f);

        information.lffSeriesRenderer = r;

        XYValueSeries lffSeries = new XYValueSeries(information.device + "-LFF");
        information.HSeries = lffSeries;

        if (cbH.isChecked()) {
            renderer.addSeriesRenderer(information.lffSeriesRenderer);
            dataSet.addSeries(devices.size(), lffSeries);
        }
    }

    public int getRandomColor() {
        Random rand = new Random();
        // Java 'Color' class takes 3 floats, from 0 to 1.
        int r = rand.nextInt(255);
        int g = rand.nextInt(255);
        int b = rand.nextInt(255);
        return new Color().rgb(r, g, b);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        for (DeviceGraphInformation information : devices) {
            XYValueSeries series;
            XYSeriesRenderer seriesRenderer;

            switch (buttonView.getId()) {
                case R.id.rb_x:
                    series = information.xSeries;
                    seriesRenderer = information.xSeriesRenderer;
                    break;
                case R.id.rb_y:
                    series = information.ySeries;
                    seriesRenderer = information.ySeriesRenderer;
                    break;
                case R.id.rb_z:
                    series = information.zSeries;
                    seriesRenderer = information.zSeriesRenderer;
                    break;
                case R.id.rb_rotation:
                    if (isChecked) {
                        seekBarFrequency.setVisibility(View.VISIBLE);
                        tvFrequency.setVisibility(View.VISIBLE);
                    } else {
                        seekBarFrequency.setVisibility(View.GONE);
                        tvFrequency.setVisibility(View.GONE);
                    }

                    series = information.HSeries;
                    seriesRenderer = information.lffSeriesRenderer;
                    break;

                case R.id.rb_sqrt:
                    series = information.sqrSeries;
                    seriesRenderer = information.sqrSeriesRenderer;
                    break;

                default:
                    series = information.HSeries;
                    seriesRenderer = information.lffSeriesRenderer;
                    break;
            }

            if (isChecked) {
                renderer.addSeriesRenderer(seriesRenderer);
                dataSet.addSeries(series);
            } else {
                renderer.removeSeriesRenderer(seriesRenderer);
                dataSet.removeSeries(series);
            }

            graphicalView.repaint();
        }
    }

    private class DeviceGraphInformation {
        private String device;

        private XYValueSeries xSeries;
        private XYValueSeries ySeries;
        private XYValueSeries zSeries;
        private XYValueSeries sqrSeries;
        private XYValueSeries HSeries;

        private org.achartengine.renderer.XYSeriesRenderer xSeriesRenderer;
        private org.achartengine.renderer.XYSeriesRenderer ySeriesRenderer;
        private org.achartengine.renderer.XYSeriesRenderer zSeriesRenderer;
        private org.achartengine.renderer.XYSeriesRenderer sqrSeriesRenderer;
        private org.achartengine.renderer.XYSeriesRenderer lffSeriesRenderer;

        public DeviceGraphInformation(String device) {
            this.device = device;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof DeviceGraphInformation)
                return this.device.equals(((DeviceGraphInformation) o).device);
            else
                return false;
        }
    }


    private String getModel(String device) {
        return device.substring(0, device.indexOf("-"));
    }


    /**
     * RoboSpice ResultTask  listener
     */
    public final class OnResultTaskListener implements RequestListener<Pits> {
        @Override
        public void onRequestFailure(SpiceException spiceException) {
            Log.i("Loger", "Error: " + spiceException.getMessage());
        }

        @Override
        public void onRequestSuccess(Pits result) {
            analyticProgress.setVisibility(View.GONE);
            analytic.setEnabled(true);

            Toast.makeText(getApplicationContext(), "Ready lines " + result.size(), Toast.LENGTH_LONG).show();

            int count = result.size();
            ArrayList<Double> pitArray = new ArrayList<Double>();
            writeServise.createFileToWriteResults(result.filename);

            //горуємо дані для запису файлу з результатами
            double s = 0;
            for (int i = 1; i < count; i++) {
                pitArray.add(result.get(i).sizeH);
                s += (i / 36) * (result.get(i).speed / 3.6);
                String data = i + " " + result.get(i).acc + " " + result.get(i).speed + " " + result.get(i).sizeH + "\n";
                writeServise.writeToFile(result.filename, data);
            }

            double sum = 0;
            double sd_dat = 0;
            for (int i = 0; i < pitArray.size(); i++) {
                sum += pitArray.get(i);
                sd_dat += (Math.pow(pitArray.get(i) - (sum / pitArray.size()), 2));
            }

            double sko_t = Math.sqrt((sd_dat / pitArray.size()));

            writeServise.writeToFile(result.filename, "\n");
            try {
                writeServise.writeToFile(result.filename, "max=" + Collections.max(pitArray) + " stDev=" + sko_t + " aVen=" + sum / result.size());
            } catch (Exception e) {
                e.printStackTrace();
            }
            writeServise.stopWriteToFile();
        }
    }

}
