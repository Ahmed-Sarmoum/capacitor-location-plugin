import Foundation
import Capacitor
import CoreLocation
import UIKit

@objc(LocationPluginPlugin)
public class LocationPluginPlugin: CAPPlugin, CAPBridgedPlugin, CLLocationManagerDelegate {

    public let identifier = "LocationPluginPlugin"
    public let jsName = "LocationPlugin"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "initialize", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isEnabled", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "checkMock", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "openDeveloperSettings", returnType: CAPPluginReturnPromise)
    ]

    private var locationManager: CLLocationManager?
    private var lastIsEnabled = false
    private var activeProbes: [AnyObject] = []

    @objc func initialize(_ call: CAPPluginCall) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            self.locationManager = CLLocationManager()
            self.locationManager?.delegate = self

            self.updateLocationStatus(shouldNotify: false)

            call.resolve([
                "isEnabled": self.lastIsEnabled
            ])
        }
    }

    @objc func isEnabled(_ call: CAPPluginCall) {
        call.resolve([
            "isEnabled": lastIsEnabled
        ])
    }

    @objc func checkMock(_ call: CAPPluginCall) {
        guard #available(iOS 15.0, *) else {
            call.resolve(["isMock": false, "available": false])
            return
        }

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            let status: CLAuthorizationStatus
            if #available(iOS 14.0, *) {
                status = (self.locationManager ?? CLLocationManager()).authorizationStatus
            } else {
                status = CLLocationManager.authorizationStatus()
            }

            guard status == .authorizedWhenInUse || status == .authorizedAlways else {
                call.resolve(["isMock": false, "available": false])
                return
            }

            let probe = MockLocationProbe()
            self.activeProbes.append(probe)
            probe.start { [weak self] isMock, available in
                call.resolve(["isMock": isMock, "available": available])
                self?.activeProbes.removeAll { $0 === probe }
            }
        }
    }

    @objc func openDeveloperSettings(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            if let url = URL(string: UIApplication.openSettingsURLString) {
                UIApplication.shared.open(url, options: [:], completionHandler: nil)
            }
            call.resolve()
        }
    }

    private func updateLocationStatus(shouldNotify: Bool) {
        let authStatus: CLAuthorizationStatus

        if #available(iOS 14.0, *) {
            authStatus = locationManager?.authorizationStatus ?? .notDetermined
        } else {
            authStatus = CLLocationManager.authorizationStatus()
        }

        let isServicesEnabled = CLLocationManager.locationServicesEnabled()
        let isAuthorized = (authStatus == .authorizedWhenInUse || authStatus == .authorizedAlways)
        let newStatus = isServicesEnabled && isAuthorized

        if newStatus != lastIsEnabled {
            lastIsEnabled = newStatus

            if shouldNotify {
                notifyListeners("locationStatusChanged", data: [
                    "isEnabled": lastIsEnabled
                ])
            }
        } else {
            lastIsEnabled = newStatus
        }
    }

    // MARK: - CLLocationManagerDelegate

    public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        updateLocationStatus(shouldNotify: true)
    }

    public func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        updateLocationStatus(shouldNotify: true)
    }

    deinit {
        locationManager?.delegate = nil
        locationManager = nil
    }
}

@available(iOS 15.0, *)
private class MockLocationProbe: NSObject, CLLocationManagerDelegate {

    private var manager: CLLocationManager?
    private var completion: ((Bool, Bool) -> Void)?
    private var finished = false
    private var timeoutWork: DispatchWorkItem?

    func start(timeout: TimeInterval = 4.0, completion: @escaping (Bool, Bool) -> Void) {
        self.completion = completion

        let manager = CLLocationManager()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
        self.manager = manager

        let work = DispatchWorkItem { [weak self] in
            self?.finish(isMock: false, available: true)
        }
        timeoutWork = work
        DispatchQueue.main.asyncAfter(deadline: .now() + timeout, execute: work)

        manager.requestLocation()
    }

    private func finish(isMock: Bool, available: Bool) {
        guard !finished else { return }
        finished = true
        timeoutWork?.cancel()
        manager?.delegate = nil
        manager = nil
        completion?(isMock, available)
        completion = nil
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else {
            finish(isMock: false, available: true)
            return
        }

        let isMock = location.sourceInformation?.isSimulatedBySoftware ?? false
        finish(isMock: isMock, available: true)
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        finish(isMock: false, available: true)
    }
}
