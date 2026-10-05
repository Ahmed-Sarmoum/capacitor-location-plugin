package com.ahmed.plugins.locations;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LocationPluginPluginTest {

    private static final String GPS = LocationManager.GPS_PROVIDER;
    private static final String NETWORK = LocationManager.NETWORK_PROVIDER;

    private Application context;
    private LocationManager locationManager;
    private FusedLocationProviderClient fused;
    private MockedStatic<LocationServices> locationServices;
    private LocationPluginPlugin plugin;
    private PluginCall call;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        shadowOf(context).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        shadowOf(locationManager).setProviderEnabled(GPS, true);
        shadowOf(locationManager).setProviderEnabled(NETWORK, false);

        // No Play Services on the JVM: hand the plugin a fake fused client.
        fused = mock(FusedLocationProviderClient.class);
        locationServices = mockStatic(LocationServices.class);
        locationServices.when(() -> LocationServices.getFusedLocationProviderClient(any(Context.class))).thenReturn(fused);

        plugin = spy(new LocationPluginPlugin());
        doReturn(context).when(plugin).getContext();
        call = mock(PluginCall.class);
    }

    @After
    public void tearDown() {
        locationServices.close();
    }

    @Test
    public void noPermission_unavailable() {
        shadowOf(context).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);

        plugin.getVerifiedPosition(call);

        assertUnavailable(resolved());
    }

    @Test
    public void fusedFix_returnsCoordinates() {
        fusedReturns(Tasks.forResult(fix("fused", false)));

        plugin.getVerifiedPosition(call);

        JSObject ret = resolved();
        assertTrue(ret.optBoolean("available"));
        assertFalse(ret.optBoolean("isMock"));
        assertEquals(36.75, ret.optDouble("latitude"), 0);
        assertEquals(3.06, ret.optDouble("longitude"), 0);
        assertEquals(5.0, ret.optDouble("accuracy"), 0);
        assertEquals(1_700_000_000_000L, ret.optLong("time"));
    }

    @Test
    public void fusedMockFix_flaggedMock() {
        fusedReturns(Tasks.forResult(fix("fused", true)));

        plugin.getVerifiedPosition(call);

        JSObject ret = resolved();
        assertTrue(ret.optBoolean("available"));
        assertTrue(ret.optBoolean("isMock"));
    }

    @Test
    public void fusedEmpty_lingeringMockLastKnown_returned() {
        fusedReturns(Tasks.forResult(null));
        shadowOf(locationManager).setLastKnownLocation(GPS, fix(GPS, true));

        plugin.getVerifiedPosition(call);

        JSObject ret = resolved();
        assertTrue(ret.optBoolean("available"));
        assertTrue(ret.optBoolean("isMock"));
        assertEquals(36.75, ret.optDouble("latitude"), 0);
    }

    @Test
    public void fusedFails_singleGpsFixReturned() {
        fusedReturns(Tasks.forException(new Exception("fused unavailable")));

        plugin.getVerifiedPosition(call);
        shadowOf(Looper.getMainLooper()).idle(); // let the fallback register its listener
        shadowOf(locationManager).simulateLocation(fix(GPS, false));

        JSObject ret = resolved();
        assertTrue(ret.optBoolean("available"));
        assertFalse(ret.optBoolean("isMock"));
        assertEquals(36.75, ret.optDouble("latitude"), 0);
    }

    @Test
    public void noFixBeforeTimeout_unavailable() {
        fusedReturns(Tasks.forException(new Exception("fused unavailable")));

        plugin.getVerifiedPosition(call);

        assertUnavailable(resolved());
    }

    @Test
    public void noProviderEnabled_unavailable() {
        fusedReturns(Tasks.forException(new Exception("fused unavailable")));
        shadowOf(locationManager).setProviderEnabled(GPS, false);

        plugin.getVerifiedPosition(call);

        assertUnavailable(resolved());
    }

    // Regression: lastKnownIsMock -> lastKnownMock refactor.
    @Test
    public void checkMockLastKnown_flagsLingeringMock() {
        shadowOf(locationManager).setLastKnownLocation(GPS, fix(GPS, true));

        plugin.checkMockLastKnown(call);

        JSObject ret = resolved();
        assertTrue(ret.optBoolean("available"));
        assertTrue(ret.optBoolean("isMock"));
    }

    private void fusedReturns(Task<Location> task) {
        when(fused.getCurrentLocation(any(CurrentLocationRequest.class), any(CancellationToken.class))).thenReturn(task);
    }

    // Runs everything queued on the main looper past the 4 s timeout, then
    // checks the call was resolved exactly once.
    private JSObject resolved() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5));
        ArgumentCaptor<JSObject> captor = ArgumentCaptor.forClass(JSObject.class);
        verify(call).resolve(captor.capture());
        return captor.getValue();
    }

    private static void assertUnavailable(JSObject ret) {
        assertFalse(ret.optBoolean("available", true));
        assertFalse(ret.optBoolean("isMock", true));
        assertFalse(ret.has("latitude"));
    }

    private static Location fix(String provider, boolean mock) {
        Location location = new Location(provider);
        location.setLatitude(36.75);
        location.setLongitude(3.06);
        location.setAccuracy(5f);
        location.setTime(1_700_000_000_000L);
        location.setMock(mock);
        return location;
    }
}
