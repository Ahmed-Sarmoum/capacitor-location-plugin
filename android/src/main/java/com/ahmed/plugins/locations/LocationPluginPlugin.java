package com.ahmed.plugins.locations;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.ArrayList;
import java.util.List;

@CapacitorPlugin(name = "LocationPlugin")
public class LocationPluginPlugin extends Plugin {

    private static final long MOCK_FIX_TIMEOUT_MS = 4000;

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

    @PluginMethod
    public void checkMock(PluginCall call) {
        Context context = getContext();

        if (!hasLocationPermission(context)) {
            resolveMock(call, false, false);
            return;
        }

        requestFusedFix(context, call);
    }

    private void requestFusedFix(Context context, PluginCall call) {
        FusedLocationProviderClient fused;

        try {
            fused = LocationServices.getFusedLocationProviderClient(context);
        } catch (Throwable t) {
            fallbackToLocationManager(context, call);
            return;
        }

        CurrentLocationRequest request = new CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0)
            .setDurationMillis(MOCK_FIX_TIMEOUT_MS)
            .build();

        try {
            fused
                .getCurrentLocation(request, new CancellationTokenSource().getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        resolveMock(call, locationIsMock(location), true);
                    } else {
                        fallbackToLocationManager(context, call);
                    }
                })
                .addOnFailureListener(e -> fallbackToLocationManager(context, call));
        } catch (SecurityException e) {
            fallbackToLocationManager(context, call);
        }
    }

    private void fallbackToLocationManager(Context context, PluginCall call) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (lastKnownIsMock(locationManager)) {
            resolveMock(call, true, true);
            return;
        }

        requestSingleFix(locationManager, call);
    }

    @PluginMethod
    public void openDeveloperSettings(PluginCall call) {
        if (openSettings(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) || openSettings(Settings.ACTION_SETTINGS)) {
            call.resolve();
        } else {
            call.reject("Unable to open developer settings");
        }
    }

    private boolean openSettings(String action) {
        try {
            Intent intent = new Intent(action);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasLocationPermission(Context context) {
        return context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean locationIsMock(Location location) {
        if (location == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return location.isMock();
        return location.isFromMockProvider();
    }

    private boolean lastKnownIsMock(LocationManager locationManager) {
        for (String provider : locationManager.getAllProviders()) {
            try {
                if (locationIsMock(locationManager.getLastKnownLocation(provider))) return true;
            } catch (SecurityException ignored) {
                // Provider not accessible under current permissions
            }
        }
        return false;
    }

    private void requestSingleFix(LocationManager locationManager, PluginCall call) {
        List<String> providers = new ArrayList<>();
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) providers.add(LocationManager.GPS_PROVIDER);
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) providers.add(LocationManager.NETWORK_PROVIDER);

        if (providers.isEmpty()) {
            resolveMock(call, false, true);
            return;
        }

        final boolean[] settled = { false };
        final Handler handler = new Handler(Looper.getMainLooper());
        final List<LocationListener> listeners = new ArrayList<>();

        final Runnable cleanup = () -> {
            for (LocationListener listener : listeners) {
                try {
                    locationManager.removeUpdates(listener);
                } catch (SecurityException ignored) {
                }
            }
        };

        final Runnable timeout = () -> {
            if (settled[0]) return;
            settled[0] = true;
            cleanup.run();
            resolveMock(call, false, true);
        };

        for (String provider : providers) {
            LocationListener listener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (settled[0]) return;
                    settled[0] = true;
                    handler.removeCallbacks(timeout);
                    cleanup.run();
                    resolveMock(call, locationIsMock(location), true);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}
            };

            listeners.add(listener);

            try {
                locationManager.requestLocationUpdates(provider, 0, 0, listener, Looper.getMainLooper());
            } catch (SecurityException ignored) {
                // Permission revoked mid-call
            }
        }

        handler.postDelayed(timeout, MOCK_FIX_TIMEOUT_MS);
    }

    private void resolveMock(PluginCall call, boolean isMock, boolean available) {
        JSObject ret = new JSObject();
        ret.put("isMock", isMock);
        ret.put("available", available);
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