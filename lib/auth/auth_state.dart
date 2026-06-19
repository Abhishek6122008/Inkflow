import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../api/api_client.dart';

final apiClientProvider = Provider<ApiClient>((ref) => ApiClient());

class AuthUser {
  AuthUser({required this.id, required this.email, required this.displayName});

  final String id;
  final String email;
  final String displayName;

  factory AuthUser.fromJson(Map<String, dynamic> json) => AuthUser(
        id: json['id'] as String,
        email: json['email'] as String,
        displayName: json['displayName'] as String,
      );
}

class AuthState {
  const AuthState({this.user, this.isLoading = true});

  final AuthUser? user;
  final bool isLoading;

  bool get isLoggedIn => user != null;

  AuthState copyWith({AuthUser? user, bool? isLoading}) => AuthState(
        user: user ?? this.user,
        isLoading: isLoading ?? this.isLoading,
      );
}

class AuthController extends StateNotifier<AuthState> {
  AuthController(this._api) : super(const AuthState()) {
    _restore();
  }

  final ApiClient _api;

  Future<void> _restore() async {
    final token = await _api.readToken();
    state = AuthState(user: null, isLoading: false);
    if (token == null) return;
    // Token presence alone is enough for the demo; a real app would
    // validate/refresh here. Leave user as null until login populates it
    // so the router still requires an explicit login for the demo flow.
  }

  Future<void> register({
    required String email,
    required String password,
    required String displayName,
  }) async {
    await _api.register(email: email, password: password, displayName: displayName);
  }

  Future<void> login({required String email, required String password}) async {
    final response = await _api.login(email: email, password: password);
    await _api.saveToken(response['accessToken'] as String);
    state = state.copyWith(user: AuthUser.fromJson(response['user'] as Map<String, dynamic>));
  }

  Future<void> logout() async {
    await _api.clearToken();
    state = const AuthState(user: null, isLoading: false);
  }
}

final authControllerProvider = StateNotifierProvider<AuthController, AuthState>((ref) {
  return AuthController(ref.watch(apiClientProvider));
});
