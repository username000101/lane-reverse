# Lane Flutter — Нативное мобильное и десктопное приложение

Красивый нативный кроссплатформенный клиент для музыкального сервиса Lane на Flutter.

- **Telegram Auth Flow**: Вход без паролей и логинов через официального бота `@lane_music_bot` с отрисовкой контрастного QR-кода и ссылкой прямого перехода.
- **Подпись BNIT на чистом Dart**: Алгоритм `libbnit.so` (HMAC-SHA256, кастомный Base64, мутирующий RC4 и envelope-шифр) полностью портирован на чистый Dart без внешних C/Rust FFI-зависимостей — приложение компилируется и работает везде (Android, iOS, macOS, Windows, Linux).
- **Плеер и стриминг**: Нативное аудиовоспроизведение через `just_audio` с бесшовным переключением качества (`BASIC 128k`, `HIGH 192k`, `ULTRA 320k`).
- **Экспорт конфига**: Возможность в один клик скопировать `laned.json` для консольных утилит (`crates/laned-cli`, `crates/laned-tui`).

---

## Архитектура проекта

```text
app/
├── lib/
│   ├── main.dart                          # Точка входа, переключение экранов Auth/Library
│   ├── core/
│   │   ├── constants/api_constants.dart   # Эндпоинты Lane API и конфигурация
│   │   ├── crypto/bnit.dart               # Порт BNIT подписи запросов на чистом Dart
│   │   ├── models/                        # Модели User, Playlist, Track
│   │   ├── network/lane_api_client.dart   # Подписанный HTTP-клиент
│   │   ├── storage/session_storage.dart   # Хранилище сессии и экспорт laned.json
│   │   └── theme/app_theme.dart           # Тёмная тема (Obsidian & Cyan)
│   └── features/
│       ├── auth/                          # Экран входа через бота с QR и радаром
│       ├── library/                       # Библиотека треков, плейлисты и поиск
│       ├── player/                        # Мини-плеер и полноэкранный плеер
│       └── settings/                      # Модалка конфига сессии и выхода
├── test/
│   └── bnit_test.dart                     # Сверка алгоритма с golden-векторами Rust/C++
└── pubspec.yaml
```

---

## Запуск приложения

Убедитесь, что у вас установлен Flutter SDK (`flutter --version`):

```bash
cd app

# Установка зависимостей
flutter pub get

# Запуск тестов подписи BNIT
flutter test test/bnit_test.dart

# Запуск приложения на подключенном устройстве / эмуляторе
flutter run
```

---

## Поддерживаемые платформы
- **Android** (API 21+)
- **iOS** (iOS 12+)
- **Windows** (10+)
- **macOS** (10.14+)
- **Linux**
