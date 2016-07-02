package com.stfalcon.client.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.stfalcon.client.MyApplication;
import com.stfalcon.client.R;
import com.stfalcon.client.SensorService;

public class ClientActivity extends Activity implements View.OnClickListener {

    private Button start, stop, client;
    private RadioButton accel, lAccel, gravity;
    private ServiceConnection sConn;
    private SensorService writeServise;
    private boolean bound = false;
    private Intent intentService;
    private BroadcastReceiver mReceiver;
    private ImageView conState;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        conState = (ImageView) findViewById(R.id.connect_state);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        client = (Button) findViewById(R.id.connect);
        accel = (RadioButton) findViewById(R.id.type_a);
        lAccel = (RadioButton) findViewById(R.id.type_la);
        gravity = (RadioButton) findViewById(R.id.type_g);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        client.setOnClickListener(this);
        accel.setOnClickListener(this);
        lAccel.setOnClickListener(this);
        gravity.setOnClickListener(this);

        // no sleep fot this screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        switch (MyApplication.getInstance().getSendedType()) {
            case Sensor.TYPE_ACCELEROMETER:
                accel.setChecked(true);
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                lAccel.setChecked(true);
                break;
            case Sensor.TYPE_GRAVITY:
                gravity.setChecked(true);
                break;
        }


        intentService = new Intent(this, SensorService.class);

        //Connect location service
        sConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.d("Loger", "MainActivity onServiceConnected");
                writeServise = ((SensorService.WriteBinder) binder).getService();
                bound = true;
            }

            public void onServiceDisconnected(ComponentName name) {
                Log.d("Loger", "MainActivity onServiceDisconnected");
                bound = false;
            }
        };


        SeekBar seekBar = (SeekBar) findViewById(R.id.seekbar_speed);
        final TextView tv_speed = (TextView) findViewById(R.id.tv_speed);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                tv_speed.setText(String.valueOf(writeServise.validateSpeed(seekBar.getProgress())));
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.info:
                showInfoDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
    public void onClick(View view) {
        if (bound) {
            switch (view.getId()) {
                case R.id.start:
                    Log.i("Loger", "start");
                    startService(new Intent(this, SensorService.class));
                    writeServise.startListening();
                    break;

                case R.id.stop:
                    Log.i("Loger", "stop");
                    writeServise.stopListening();
                    break;

                case R.id.connect:
                    writeServise.connect();
                    break;

                case R.id.type_a:
                    MyApplication.getInstance().setSendetType(Sensor.TYPE_ACCELEROMETER);
                    break;

                case R.id.type_la:
                    MyApplication.getInstance().setSendetType(Sensor.TYPE_LINEAR_ACCELERATION);
                    break;

                case R.id.type_g:
                    MyApplication.getInstance().setSendetType(Sensor.TYPE_GRAVITY);
                    break;

            }
        }
    }


    /**
     * Create receiver for cache data from services
     */
    private void registerReceiver() {

        IntentFilter intentFilter = new IntentFilter(MyApplication.CONNECTED);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.hasExtra(MyApplication.WIFI)) {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    return;
                }

                if (intent.hasExtra(MyApplication.DEVICE)) {
                    conState.setImageResource(android.R.drawable.star_big_on);
                    Toast.makeText(ClientActivity.this,
                            getString(R.string.connected),
                            Toast.LENGTH_LONG).show();
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, intentFilter);
    }


    private void showInfoDialog() {
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        String sensorInfo = "TYPE_ACCELEROMETER - " + sensor.getName();
        sensorInfo = sensorInfo + "\n" + "Version - " + sensor.getVersion();
        sensorInfo = sensorInfo + "\n" + "Type - " + sensor.getType();
        sensorInfo = sensorInfo + "\n" + "Vendor - " + sensor.getVendor();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            sensorInfo = sensorInfo + "\n" + "FifoMaxEventCount - " + sensor.getFifoMaxEventCount();
            sensorInfo = sensorInfo + "\n" + "FifoReservedEventCount - " + sensor.getFifoReservedEventCount();
        }
        sensorInfo = sensorInfo + "\n" + "MaximumRange - " + sensor.getMaximumRange();
        sensorInfo = sensorInfo + "\n" + "MinDelay - " + sensor.getMinDelay() + " microsecond";
        sensorInfo = sensorInfo + "\n" + "Power - " + sensor.getPower();
        sensorInfo = sensorInfo + "\n" + "Resolution - " + sensor.getResolution();


        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString(R.string.info));
        alertDialog.setMessage(sensorInfo);
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });
        alertDialog.setIcon(android.R.drawable.ic_dialog_info);
        alertDialog.show();

    }

}
