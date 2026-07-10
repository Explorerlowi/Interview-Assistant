import SwiftUI
import shared

@main
struct iOSApp: App {
    init() {
        // Init Koin
        HelperKt.doInitKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
