package com.ahmed.plugins.locations;

import android.util.Log;

public class LocationPlugin {

    public String echo(String value) {
        Log.i("Echo", value);
        return value;
    }
}
