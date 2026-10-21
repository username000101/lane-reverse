# Lane Web — морда с прокси и входом через Telegram

Неофициальный веб-клиент Lane: красивый лендинг, библиотека плейлистов и
плеер в браузере. Браузер не знает ни JWT, ни ключа подписи — всё живёт на
сервере, в обычных Vercel Functions.

## Как это работает

```
браузер ──► /api/auth/start ──► device id + ссылка t.me/lane_music_bot?start=auth<id>
   │
   ├──► пользователь жмёт Start в официальном боте
   │
   ├──► /api/auth/poll ──► GET /auth/<id> ──► { token, user }
   │                         (подпись, LDI = id, без Bearer)
   │
   └──► сессия: AES-256-GCM в HttpOnly-cookie
              │
              └──► /api/playlists, /api/playlist, /api/stream
                       └──► подписанный запрос к https://ru.laneapi.com
                            (x-core-token, x-client-meta, x-request-trace-id)
```

Подпись — порт `crates/laned-bnit` на Node.js (`api/_lib/bnit.js`), сверен с
golden-векторами из Rust-тестов: HMAC-SHA256, RC4 с мутацией S-бокса,
кастомный base64 и «шифр конверта» с переносом.

## Структура

```
public/            статика: index.html, styles.css, app.js, favicon.svg
api/_lib/          bnit.js (подпись), lane.js (fetch к API), session.js (cookie)
api/auth/          start, poll, logout
api/               me, playlists, playlist, stream, health
scripts/dev-server.js  локальный сервер: статика + те же функции
test/bnit.test.js  сверка с golden-векторами
```

## Переменные окружения

| Имя | Обязательна | Что |
|---|---|---|
| `LANE_SESSION_SECRET` | да | ключ шифрования cookie (AES-256-GCM); без неё используется dev-заглушка |
| `LANE_API_BASE` | нет | по умолчанию `https://ru.laneapi.com` |
| `LANE_TIMEOUT_MS` | нет | таймаут запроса к API, по умолчанию 12000 |

Секрет можно сгенерировать так:

```bash
node -e "console.log(require('crypto').randomBytes(48).toString('base64url'))"
```

## Локально

```bash
node scripts/dev-server.js --port 4173
# http://127.0.0.1:4173
node test/bnit.test.js
```

Локально `LANE_SESSION_SECRET` подставляется заглушкой, а `Secure`-cookie
работает на `localhost` в Chrome/Edge. Для проверки полного входа нужен
реальный Telegram-аккаунт.

## Деплой

Проект — обычная статика плюс Node-функции, без сборки и зависимостей:

```bash
vercel deploy --prod
```

На Vercel: Root Directory — `web`, Framework Preset — Other, Build Command —
пусто, Output Directory — `public` (подхватывается автоматически).

## Безопасность и ограничения

- Токен Lane хранится только в зашифрованной HttpOnly-cookie, в браузерном
  JavaScript его нет; localStorage используется лишь для качества и громкости.
- Ключ подписи и `x-core-token` генерируются на сервере; CORS не нужен.
- `device id` одноразовый для каждой попытки входа и живёт 20 минут.
- Это неофициальный клиент: используйте только свой аккаунт и соблюдайте
  правила сервиса.
