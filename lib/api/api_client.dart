import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

const _baseUrl = 'http://localhost:8080';
const _tokenKey = 'access_token';

class ApiClient {
  ApiClient() : dio = Dio(BaseOptions(baseUrl: _baseUrl)) {
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          final token = await _storage.read(key: _tokenKey);
          if (token != null) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          handler.next(options);
        },
      ),
    );
  }

  final Dio dio;
  final _storage = const FlutterSecureStorage();

  Future<void> saveToken(String token) => _storage.write(key: _tokenKey, value: token);

  Future<String?> readToken() => _storage.read(key: _tokenKey);

  Future<void> clearToken() => _storage.delete(key: _tokenKey);

  Future<Map<String, dynamic>> register({
    required String email,
    required String password,
    required String displayName,
  }) async {
    final response = await dio.post('/api/auth/register', data: {
      'email': email,
      'password': password,
      'displayName': displayName,
    });
    return response.data as Map<String, dynamic>;
  }

  Future<Map<String, dynamic>> login({
    required String email,
    required String password,
  }) async {
    final response = await dio.post('/api/auth/login', data: {
      'email': email,
      'password': password,
    });
    return response.data as Map<String, dynamic>;
  }

  Future<List<dynamic>> listDocuments() async {
    final response = await dio.get('/api/documents');
    return response.data as List<dynamic>;
  }

  Future<Map<String, dynamic>> createDocument(String title) async {
    final response = await dio.post('/api/documents', data: {'title': title});
    return response.data as Map<String, dynamic>;
  }

  Future<Map<String, dynamic>> getDocument(String id) async {
    final response = await dio.get('/api/documents/$id');
    return response.data as Map<String, dynamic>;
  }

  Future<Map<String, dynamic>> saveDocumentContent(String id, String content) async {
    final response = await dio.put('/api/documents/$id/content', data: {'content': content});
    return response.data as Map<String, dynamic>;
  }
}

/// Extracts a human-readable message from a backend ErrorResponse, falling
/// back to the raw exception message if the response isn't in that shape.
String apiErrorMessage(Object error) {
  if (error is DioException) {
    final data = error.response?.data;
    if (data is Map && data['message'] is String) {
      return data['message'] as String;
    }
    return error.message ?? 'Network error';
  }
  return error.toString();
}
