import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:umlpilot_mobile/core/constants/api_constants.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('lee y normaliza las variables del .env', () {
    dotenv.loadFromString(envString: '''
API_BASE_URL=http://10.0.2.2:8080/
APP_NAME=UML Pilot Test
WEB_BASE_URL=https://web.umlpilot.test/
''');

    expect(ApiConstants.baseUrl, 'http://10.0.2.2:8080');
    expect(ApiConstants.appName, 'UML Pilot Test');
    expect(ApiConstants.webBaseUrl, 'https://web.umlpilot.test');
    expect(ApiConstants.wsUrl, 'ws://10.0.2.2:8080/ws');
  });

  test('deriva wss cuando el backend usa https', () {
    dotenv.loadFromString(envString: '''
API_BASE_URL=https://api.umlpilot.test
APP_NAME=UML Pilot
WEB_BASE_URL=https://umlpilot.test
''');

    expect(ApiConstants.wsUrl, 'wss://api.umlpilot.test/ws');
  });
}
