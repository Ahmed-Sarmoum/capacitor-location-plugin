import Foundation
import Capacitor
import CoreLocation
import UserNotifications

@objc(LocationPlugin)
public class LocationPlugin: CAPPlugin {
    private var locationManager = CLLocationManager()

    override public func load() {
        super.load()
        locationManager.delegate = self

        // Request permission for notifications
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { granted, error in
            if let error = error {
                print("Notification permission error: \(error.localizedDescription)")
            }
        }
    }

    @objc func isLocationEnabled(_ call: CAPPluginCall) {
        // Ensure location services are enabled
        guard CLLocationManager.locationServicesEnabled() else {
            call.resolve(["isEnabled": false])
            return
        }

        let status = CLLocationManager.authorizationStatus()
        let isEnabled = (status == .authorizedWhenInUse || status == .authorizedAlways)

        let result = ["isEnabled": isEnabled]
        call.resolve(result)

        // Request authorization if not already enabled
        if !isEnabled {
            locationManager.requestWhenInUseAuthorization()
        } else {
            // Only start updating if authorization is granted
            locationManager.startUpdatingLocation()
        }
    }

    public func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        var isEnabled = false
        
        switch status {
        case .authorizedWhenInUse, .authorizedAlways:
            isEnabled = true
            locationManager.startUpdatingLocation() // Start updating if authorized
        case .denied, .restricted:
            locationManager.stopUpdatingLocation() // Stop if not authorized
            showLocationAccessDeniedNotification() // Notify user
        default:
            break
        }
        
        let result = ["isEnabled": isEnabled]
        notifyListeners("locationStatusChanged", data: result)
    }

    private func showLocationAccessDeniedNotification() {
        let content = UNMutableNotificationContent()
        content.title = "Location Access Denied"
        content.body = "Please enable location access in the settings to use location features."
        content.sound = UNNotificationSound.default

        let request = UNNotificationRequest(identifier: "locationAccessDenied", content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("Error displaying notification: \(error.localizedDescription)")
            }
        }
    }
}

// CLLocationManagerDelegate
extension LocationPlugin: CLLocationManagerDelegate {
    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        if let location = locations.last {
            let result = ["latitude": location.coordinate.latitude, "longitude": location.coordinate.longitude]
            notifyListeners("locationUpdated", data: result)
        }
    }

    public func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        // Handle location errors gracefully
        print("Location update failed with error: \(error.localizedDescription)")
    }
}
