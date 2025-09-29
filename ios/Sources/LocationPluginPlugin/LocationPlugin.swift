import Foundation
import Capacitor
import CoreLocation

@objc(LocationPluginPlugin)
public class LocationPluginPlugin: CAPPlugin, CLLocationManagerDelegate {
    
    private var locationManager: CLLocationManager?
    private var lastIsEnabled = false
    
    @objc func initialize(_ call: CAPPluginCall) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            
            // Initialize location manager
            self.locationManager = CLLocationManager()
            self.locationManager?.delegate = self
            
            // Check initial status
            self.updateLocationStatus(shouldNotify: false)
            
            call.resolve([
                "isEnabled": self.lastIsEnabled
            ])
            
            print("Location initialized: \(self.lastIsEnabled)")
        }
    }
    
    @objc func isEnabled(_ call: CAPPluginCall) {
        call.resolve([
            "isEnabled": lastIsEnabled
        ])
    }
    
    private func updateLocationStatus(shouldNotify: Bool) {
        let authStatus: CLAuthorizationStatus
        
        if #available(iOS 14.0, *) {
            authStatus = locationManager?.authorizationStatus ?? .notDetermined
        } else {
            authStatus = CLLocationManager.authorizationStatus()
        }
        
        // Check if location services are enabled globally and authorized for this app
        let isServicesEnabled = CLLocationManager.locationServicesEnabled()
        let isAuthorized = (authStatus == .authorizedWhenInUse || authStatus == .authorizedAlways)
        let newStatus = isServicesEnabled && isAuthorized
        
        // Only notify if status actually changed
        if newStatus != lastIsEnabled {
            lastIsEnabled = newStatus
            
            if shouldNotify {
                print("LocationPlugin: Status changed to: \(lastIsEnabled)")
                notifyListeners("locationStatusChanged", data: [
                    "isEnabled": lastIsEnabled
                ])
            }
        } else {
            // Status didn't change, just update the cache silently
            lastIsEnabled = newStatus
            if shouldNotify {
                print("LocationPlugin: Status unchanged: \(lastIsEnabled)")
            }
        }
    }
    
    // MARK: - CLLocationManagerDelegate
    
    public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        updateLocationStatus(shouldNotify: true)
    }
    
    // For iOS 13 and earlier
    public func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        updateLocationStatus(shouldNotify: true)
    }
    
    deinit {
        locationManager?.delegate = nil
        locationManager = nil
    }
}