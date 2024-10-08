package com.ahmed.plugins.locations;


import android.content.Context;
import android.location.LocationManager;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "LocationPlugin")
public class LocationPluginPlugin extends Plugin {

    @PluginMethod
    public void isLocationEnabled(PluginCall call) {
        Context context = getContext();
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
        JSObject ret = new JSObject();
        ret.put("enabled", isGpsEnabled || isNetworkEnabled);
        call.resolve(ret);
    }
}
