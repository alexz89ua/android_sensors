package com.stfalcon.server.view;

import android.app.Activity;
import com.octo.android.robospice.SpiceManager;
import com.stfalcon.server.service.RoadSpiceService;

/**
 * Base class for all activity on project
 */
public abstract class BaseSpiceActivity extends Activity {


    private SpiceManager spiceManager = new SpiceManager(RoadSpiceService.class);

    public SpiceManager getSpiceManager() {
        return spiceManager;
    }


    @Override
    public void onStart() {
        spiceManager.start(this);
        super.onStart();
    }


    @Override
    public void onStop() {
        spiceManager.shouldStop();
        super.onStop();
    }
}

