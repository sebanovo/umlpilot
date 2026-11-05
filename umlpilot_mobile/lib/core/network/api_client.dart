import 'dart:async';
import 'dart:typed_data';

import 'package:dio/dio.dart';

import '../constants/app_constants.dart';
import '../errors/exceptions.dart';
import '../session/session_controller.dart';

/// Cliente HTTP central: autenticación Bearer, refresh single-flight y errores tipados.
class ApiClient {
  ApiClient({
    required String baseUrl,
    required SessionController session,
    Dio? dio,
  })  : _baseUrl = baseUrl,
        _session = session,
        _dio = dio ??
            Dio(
              BaseOptions(
                baseUrl: baseUrl,
                connectTimeout: AppConstants.connectTimeout,
                receiveTimeout: AppConstants.receiveTimeout,
              ),
            );

  final String _baseUrl;
  final SessionController _session;
  final Dio _dio;
  Future<bool>? _refreshing;

  Map<String, String> get _authHeaders => {
        if (_session.accessToken != null) 'Authorization': 'Bearer ${_session.accessToken}',
      };

  Options _jsonOptions({ResponseType responseType = ResponseType.json}) => Options(
        headers: {..._authHeaders, 'Content-Type': 'application/json'},
        responseType: responseType,
      );

  Future<dynamic> get(String path, {Map<String, dynamic>? query}) async {
    final response = await _send(() => _dio.get(path, queryParameters: query, options: _jsonOptions()));
    return response.data;
  }

  Future<dynamic> post(String path, {Object? data}) async {
    final response = await _send(() => _dio.post(path, data: data, options: _jsonOptions()));
    return response.data;
  }

  Future<dynamic> put(String path, {Object? data}) async {
    final response = await _send(() => _dio.put(path, data: data, options: _jsonOptions()));
    return response.data;
  }

  Future<void> delete(String path) async {
    await _send(() => _dio.delete(path, options: _jsonOptions()));
  }

  Future<dynamic> postMultipart(String path, FormData formData) async {
    final response = await _send(() => _dio.post(path, data: formData, options: Options(headers: _authHeaders)));
    return response.data;
  }

  Future<Uint8List> download(String path) async {
    final response = await _send(
      () => _dio.get<List<int>>(
        path,
        options: Options(headers: _authHeaders, responseType: ResponseType.bytes),
      ),
    );
    return Uint8List.fromList(response.data ?? const <int>[]);
  }

  Future<Response<dynamic>> _send(Future<Response<dynamic>> Function() run) async {
    try {
      return await run();
    } on DioException catch (error) {
      if (error.response?.statusCode == 401) {
        final refreshed = await _refresh();
        if (refreshed) {
          try {
            return await run();
          } on DioException catch (retryError) {
            throw _translate(retryError);
          }
        }
        await _session.expire();
        throw const UnauthorizedException();
      }
      throw _translate(error);
    }
  }

  Future<bool> _refresh() {
    final inFlight = _refreshing;
    if (inFlight != null) return inFlight;
    final future = _doRefreshSafely();
    _refreshing = future;
    return future.whenComplete(() => _refreshing = null);
  }

  Future<bool> _doRefreshSafely() async {
    try {
      final refreshToken = _session.refreshToken;
      if (refreshToken == null || refreshToken.isEmpty) return false;
      final refreshDio = Dio(
        BaseOptions(
          baseUrl: _baseUrl,
          connectTimeout: AppConstants.connectTimeout,
          receiveTimeout: AppConstants.receiveTimeout,
        ),
      );
      final response = await refreshDio.post('/api/v1/auth/refresh', data: {'refreshToken': refreshToken});
      final data = response.data;
      if (data is! Map) return false;
      final access = data['accessToken'] as String?;
      final refresh = data['refreshToken'] as String?;
      if (access == null || refresh == null) return false;
      await _session.updateTokens(accessToken: access, refreshToken: refresh);
      return true;
    } catch (_) {
      return false;
    }
  }

  AppException _translate(DioException error) {
    final response = error.response;
    if (response != null) {
      final status = response.statusCode;
      final data = response.data;
      final message = data is Map && data['error'] is String
          ? data['error'] as String
          : (response.statusMessage ?? 'Error ${status ?? ''} del servidor');
      if (status == 401) return UnauthorizedException(message);
      if (status == 403) return ForbiddenException(message);
      return ServerException(message, statusCode: status);
    }
    return const NetworkException();
  }
}
