package com.ahmed.plugins.locations;

import android.content.Context;
import android.location.LocationManager;
import android.location.LocationListener;
import android.os.Bundle;
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
        ret.put("isEnabled", isGpsEnabled || isNetworkEnabled);
        call.resolve(ret);

        // Register a listener for location changes
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
            @Override
            public void onLocationChanged(android.location.Location location) {
                // Notifying listeners when the location changes
                JSObject status = new JSObject();
                status.put("isEnabled", locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                                       locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
                notifyListeners("locationStatusChanged", status);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {
                JSObject status = new JSObject();
                status.put("isEnabled", true);
                notifyListeners("locationStatusChanged", status);
            }

            @Override
            public void onProviderDisabled(String provider) {
                JSObject status = new JSObject();
                status.put("isEnabled", false);
                notifyListeners("locationStatusChanged", status);
            }
        });
    }
}
