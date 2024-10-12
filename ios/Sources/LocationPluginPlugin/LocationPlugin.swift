import Foundation
import Capacitor
import CoreLocation

@objc(LocationPlugin)
public class LocationPlugin: CAPPlugin {
    private var locationManager = CLLocationManager()

    override public func load() {
        super.load()

        // Set delegate for location manager
        locationManager.delegate = self
    }

    @objc func isLocationEnabled(_ call: CAPPluginCall) {
        let status = CLLocationManager.authorizationStatus()
        let isEnabled = (status == .authorizedWhenInUse || status == .authorizedAlways) && CLLocationManager.locationServicesEnabled()
        
        let result = [
            "isEnabled": isEnabled
        ]
        call.resolve(result)
        
        // Start monitoring if not already
        if !isEnabled {
            locationManager.requestWhenInUseAuthorization()
        }
    }

    // Listen for changes in the authorization status
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        var isEnabled = false
        if status == .authorizedWhenInUse || status == .authorizedAlways {
            isEnabled = true
        }
        
        let result = [
            "isEnabled": isEnabled
        ] as [String : Any]
        notifyListeners("locationStatusChanged", data: result)
    }
}

// Add CLLocationManagerDelegate
extension LocationPlugin: CLLocationManagerDelegate {
    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        // Handle location updates if needed
    }
}
