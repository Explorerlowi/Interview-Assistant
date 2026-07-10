import SwiftUI
import shared

struct LoginScreen: View {
    @StateObject private var viewModel = LoginViewModelBridge()

    var body: some View {
        VStack(spacing: 16) {
            Text(NSLocalizedString("login_title", comment: ""))
                .font(.title2)

            TextField(
                NSLocalizedString("login_username", comment: ""),
                text: Binding(
                    get: { viewModel.state.username },
                    set: { viewModel.onEvent(event: LoginUiEvent.OnUsernameChanged(value: $0)) }
                )
            )
            .textFieldStyle(.roundedBorder)
            .padding(.horizontal)

            Button(action: {
                viewModel.onEvent(event: LoginUiEvent.OnLoginClicked)
            }) {
                if viewModel.state.isLoading {
                    ProgressView()
                } else {
                    Text(NSLocalizedString("login_btn", comment: ""))
                }
            }
            .disabled(viewModel.state.isLoading)

            if let error = viewModel.state.error {
                Text(error).foregroundColor(.red)
            }
        }
        .padding()
    }
}
