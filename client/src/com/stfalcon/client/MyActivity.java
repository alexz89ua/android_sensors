package com.stfalcon.client;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class MyActivity extends Activity implements View.OnClickListener {

    private Button start, stop, client;
    private RadioButton accel, lAccel, fAccel;
    private ServiceConnection sConn;
    private WriteService writeServise;
    private boolean bound = false;
    private Intent intentService;
    private BroadcastReceiver mReceiver;
    private TextView textView, tvFilterValue;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        client = (Button) findViewById(R.id.client);
        textView = (TextView) findViewById(R.id.text);
        accel = (RadioButton) findViewById(R.id.type_a);
        fAccel = (RadioButton) findViewById(R.id.type_fa);
        lAccel = (RadioButton) findViewById(R.id.type_la);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        client.setOnClickListener(this);
        accel.setOnClickListener(this);
        fAccel.setOnClickListener(this);
        lAccel.setOnClickListener(this);

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
                    startService(new Intent(this, WriteService.class));
                    writeServise.startListening();
                    break;

                case R.id.stop:
                    Log.i("Loger", "stop");
                    writeServise.stopListening();
                    break;

                case R.id.client:
                    writeServise.connect();
                    break;

                case R.id.type_a:
                    SampleApplication.getInstance().setSendetType(WriteService.TYPE_A);
                    break;

                case R.id.type_fa:
                    SampleApplication.getInstance().setSendetType(WriteService.TYPE_F);
                    break;

                case R.id.type_la:
                    SampleApplication.getInstance().setSendetType(WriteService.TYPE_L);
                    break;

            }
        }
    }


    /**
     * Create receiver for cache data from services
     */
    private void registerReceiver() {

        IntentFilter intentFilter = new IntentFilter(SampleApplication.CONNECTED);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.hasExtra(SampleApplication.WIFI)) {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    return;
                }


                if (intent.hasExtra(SampleApplication.STARTED)) {
                    textView.setText("Start listening... \n");
                    //startActivity(new Intent(MyActivity.this, GraphicActivity.class));
                    return;
                }

                if (intent.hasExtra(SampleApplication.DEVICE)) {

                    Toast.makeText(MyActivity.this,
                            intent.getStringExtra(SampleApplication.DEVICE),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MyActivity.this,
                            getString(R.string.connected),
                            Toast.LENGTH_LONG).show();
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, intentFilter);
    }

}
