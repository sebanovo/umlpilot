import 'package:get_it/get_it.dart';
import 'package:http/http.dart' as http;

import '../core/constants/api_constants.dart';
import '../core/constants/app_constants.dart';
import '../core/network/http_client.dart';
import '../features/chat/data/datasources/chat_remote_datasource.dart';
import '../features/chat/data/repositories/chat_repository_impl.dart';
import '../features/chat/domain/repositories/chat_repository.dart';
import '../features/chat/domain/usecases/send_message_usecase.dart';

final GetIt getIt = GetIt.instance;

/// Registra las dependencias de la aplicación.
Future<void> configureDependencies() async {
  getIt
    ..registerLazySingleton<http.Client>(http.Client.new)
    ..registerLazySingleton<AppHttpClient>(
      () => AppHttpClient(
        getIt<http.Client>(),
        timeout: AppConstants.requestTimeout,
      ),
    )
    ..registerLazySingleton<ChatRemoteDataSource>(
      () => OllamaChatRemoteDataSource(
        getIt<AppHttpClient>(),
        baseUrl: ApiConstants.ollamaBaseUrl,
        model: ApiConstants.ollamaModel,
      ),
    )
    ..registerLazySingleton<ChatRepository>(
      () => ChatRepositoryImpl(getIt<ChatRemoteDataSource>()),
    )
    ..registerLazySingleton<SendMessageUseCase>(
      () => SendMessageUseCase(getIt<ChatRepository>()),
    );
}
