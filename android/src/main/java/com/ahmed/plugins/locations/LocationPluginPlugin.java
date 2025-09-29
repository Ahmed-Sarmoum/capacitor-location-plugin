package com.ahmed.plugins.locations;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "LocationPlugin")
public class LocationPluginPlugin extends Plugin {

    private boolean lastIsEnabled = false;
    private BroadcastReceiver locationReceiver;

    @PluginMethod
    public void initialize(PluginCall call) {
        Context context = getContext();
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // Check initial status
        updateLocationStatus(locationManager, false); // false = don't notify on init

        JSObject ret = new JSObject();
        ret.put("isEnabled", lastIsEnabled);
        call.resolve(ret);

        Log.d("eeeeeeeeeeeee", lastIsEnabled+"");
        // Register BroadcastReceiver to listen for provider changes
        locationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {
                    LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    updateLocationStatus(lm, true); // true = notify if changed
                }
            }
        };

        IntentFilter filter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        context.registerReceiver(locationReceiver, filter);
    }

    private void updateLocationStatus(LocationManager locationManager, boolean shouldNotify) {
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean newStatus = isGpsEnabled || isNetworkEnabled;

        // Only notify if status actually changed
        if (newStatus != lastIsEnabled) {
            lastIsEnabled = newStatus;

            if (shouldNotify) {
                JSObject status = new JSObject();
                status.put("isEnabled", lastIsEnabled);
                Log.d("LocationPlugin", "Status changed to: " + lastIsEnabled);
                notifyListeners("locationStatusChanged", status);
            }
        } else {
            // Status didn't change, just update the cache silently
            lastIsEnabled = newStatus;
            if (shouldNotify) {
                Log.d("LocationPlugin", "Status unchanged: " + lastIsEnabled);
            }
        }
    }

    @PluginMethod
    public void isEnabled(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("isEnabled", lastIsEnabled);
        call.resolve(ret);
    }

    @Override
    protected void handleOnDestroy() {
        // Unregister receiver when plugin is destroyed
        if (locationReceiver != null) {
            try {
                getContext().unregisterReceiver(locationReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver was already unregistered
                Log.e("LocationPlugin", "Receiver already unregistered");
            }
        }
        super.handleOnDestroy();
    }
}