# CurrentWeatherDataBinding

Учебное Android-приложение на основе [заготовки](https://github.com/ipetrushin/CurrentWeatherDataBinding).

Один экран: ввод города, обновление погоды, температура, описание, влажность, облачность и ветер. Интерфейс использует XML Data Binding (`<data>`), запрос OpenWeatherMap выполняется в корутине Kotlin. Предусмотрена обработка ошибок.

## Запуск

1. Откройте проект в Android Studio. Используются SDK 37, AGP 9.4.1 и Gradle 9.6.0; минимальная версия Android — 7.0 (API 24).
2. В локальный `local.properties` добавьте свой ключ OpenWeatherMap:

   ```properties
   openweatherApiKey=ВАШ_КЛЮЧ
   ```

3. Выполните Gradle Sync и запустите `app` на телефоне или эмуляторе.

`local.properties`, ключи, настройки IDE и результаты сборки исключены из Git.

Проверка: `gradlew.bat assembleDebug testDebugUnitTest`.
