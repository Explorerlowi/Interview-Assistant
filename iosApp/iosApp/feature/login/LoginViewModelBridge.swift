import Foundation
import shared

/// Minimal bridge from shared LoginViewModel to SwiftUI.
///
/// Uses short-interval polling for the template. Prefer KMP-NativeCoroutines or SKIE
/// for production Flow observation.
final class LoginViewModelBridge: ObservableObject {
    private let viewModel: LoginViewModel
    private var pollTimer: Timer?

    @Published var state: LoginUiState

    init() {
        self.viewModel = LoginViewModelHelper().getLoginViewModel()
        self.state = viewModel.uiState.value
        startPolling()
    }

    deinit {
        pollTimer?.invalidate()
    }

    func onEvent(event: LoginUiEvent) {
        viewModel.onEvent(event: event)
        state = viewModel.uiState.value
    }

    private func startPolling() {
        pollTimer = Timer.scheduledTimer(withTimeInterval: 0.15, repeats: true) { [weak self] _ in
            guard let self else { return }
            self.state = self.viewModel.uiState.value
        }
    }
}
