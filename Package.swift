// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorPluginLocation",
    platforms: [.iOS(.v13)],
    products: [
        .library(
            name: "CapacitorPluginLocation",
            targets: ["LocationPluginPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", branch: "main")
    ],
    targets: [
        .target(
            name: "LocationPluginPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/LocationPluginPlugin"),
        .testTarget(
            name: "LocationPluginPluginTests",
            dependencies: ["LocationPluginPlugin"],
            path: "ios/Tests/LocationPluginPluginTests")
    ]
)