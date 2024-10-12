#import <Capacitor/Capacitor.h>

CAP_PLUGIN(LocationPlugin, "LocationPlugin",
           CAP_PLUGIN_METHOD(isLocationEnabled, CAPPluginReturnPromise);
) []