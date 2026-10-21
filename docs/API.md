# Lane HTTP API — что известно

> Полный список эндпоинтов (84, из APK) — в [API-REFERENCE.md](./API-REFERENCE.md).
> Здесь — только то, что проверено на живых запросах.

> Статус: собрано по перехваченным запросам приложения (`http-request-dumps/`),
> по контракту нативного метода `BNITManager.cdrl` и по ответам сервера на
> заведомо неверные подписи (проверено 2026-10-20).
> Это не официальная документация: часть схем ответов пока не снята.
> Сырые дампы лежат в [http-request-dumps](./http-request-dumps), оговорки к
> ним — в [его README](./http-request-dumps/README.md).

## Хост и транспорт

| Что | Значение |
| --- | --- |
| База | `https://ru.laneapi.com` |
| Протокол | HTTP/2 (curl/HTTP/1.1 тоже работает) |
| Формат | JSON; обычно сжат `gzip` (`accept-encoding: gzip`) |
| Локали | в дампах встречается только `ru`; `accept-language: ru`, `tz: Europe/Moscow` |

## Два уровня авторизации

1. **Пользовательский токен** — обычный `Authorization: Bearer <JWT>`.
   В JWT есть `sub`, `langId`, `iat`, `exp`; выдан на долгий срок.
2. **Подпись запроса** — три заголовка, которые генерирует `libbnit.so`
   через `BNITManager.cdrl`:

   | Заголовок | Формат | Назначение |
   | --- | --- | --- |
   | `x-core-token` | 215 символов base64url + завершающая `.` (всего 216) | основная подпись; привязана ко времени |
   | `x-client-meta` | 32 символа base64url | метаданные клиента |
   | `x-request-trace-id` | UUID | идентификатор запроса |

   Без них сервер отвечает `401 MISSING_SIGNATURE_TOKEN`, с битыми —
   `INVALID_SIGNATURE_TOKEN`, со старыми — `TIMESTAMP_EXPIRED`.
   Подпись короткоживущая: токены из дампов уже просрочены, поэтому
   генерировать заголовки нужно непосредственно перед запросом.

## Заголовки запроса

Обязательные/наблюдаемые:

```http
authorization: Bearer <JWT>
x-core-token: <из cdrl>
x-client-meta: <из cdrl>
x-request-trace-id: <из cdrl>
ldi: <16 hex, идентификатор установки>
x-accept-red: 0
accept-language: ru
tz: Europe/Moscow
x-app-version: 207
x-platform: android
x-theme: light
user-agent: LaneMusic/1.0 (Android; Mobile)
accept-encoding: gzip
```

В дампах `x-app-version` встречается дважды: пустой и `207`. Похоже на
особенность перехвата/клиента; рабочий вариант — `207`.

В запросе `/events` дополнительно были:

```http
x-connection-quality: FAST
x-is-vpn: false
```

Они не обязательны для остальных запросов (в дампах их нет).

## `x-accept-red` — важно

- `x-accept-red: 0` — сервер отвечает обычным JSON. **Для изучения API
  используйте 0.**
- `x-accept-red: 1` — ответ приходит в зашифрованном/упакованном виде
  («red»), обычным JSON его не прочитать. Именно так ходит само приложение,
  поэтому во всех старых дампах стоит `1`.

Заголовок влияет на формат **ответа**, а не на подпись запроса.
Практический вывод: в клиенте всегда ставьте `0`, пока не появится
необходимость разбирать формат `red`.

## Эндпоинты из дампов

Все — `GET`, если не указано иное.

| Путь | Query | Что делает |
| --- | --- | --- |
| `/events` | — | события/аналитика |
| `/platforms/v2/hints` | `q=<urlencoded>` | поисковые подсказки |
| `/user/playlists` | — | плейлисты пользователя |
| `/user/artists` | — | исполнители пользователя |
| `/user/albums` | — | альбомы пользователя |
| `/user/search/history` | — | история поиска |
| `/playlist/{id}` | — | плейлист по id; в дампе `lane_likes` |
| `/playlist/{id}/tracks` | `page=1&pageSize=50` | треки плейлиста, пагинация |

Примеры:

```http
GET /user/playlists
GET /playlist/lane_likes
GET /playlist/lane_likes/tracks?page=1&pageSize=50
GET /platforms/v2/hints?q=%D0%B7%D0%B0%D0%BF%D1%80%D0%BE%D1%81
GET /user/search/history
```

Тела запросов в дампах не сохранились (все запросы — `GET` без тела).
POST-эндпоинты, лайки, добавление в плейлист и т.п. пока не исследованы.

## Ошибки

Формат тела:

```json
{
  "code": "MISSING_SIGNATURE_TOKEN",
  "message": "Missing tokens",
  "requestId": "..."
}
```

Дублируются заголовками `X-Error-Code` и `X-Request-Id`.

| HTTP | `code` | Причина |
| --- | --- | --- |
| 401 | `MISSING_SIGNATURE_TOKEN` | нет одного из трёх заголовков подписи |
| 401 | `INVALID_SIGNATURE_TOKEN` | битый формат/подпись |
| 401 | `TIMESTAMP_EXPIRED` | подпись старше допустимого окна |

`requestId` полезно логировать: по нему поддержка ищет запрос.

## Как получить подпись

```text
libbnit.so (unidbg)
  └─ BNITManager.cdrl(method, path, query, body, timestamp)
       └─ String[]: "x-client-meta:...", "x-request-trace-id:...", "x-core-token:..."
```

`path` и `query` передаются отдельно; если в CLI путь содержит `?`,
загрузчик сам разделяет его. `body` — `byte[]` (пустой для GET),
`timestamp` — `0` (нативный код сам подставляет текущее время — это
наблюдение, отдельно не проверялось).

Готовый цикл:

```bash
# 1. подпись (JSON в stdout)
java -jar lane-unidbg-*-all.jar ./libs GET /user/playlists

# 2. запрос с x-accept-red: 0
#    (см. ts-example/index.ts)
```

## Что ещё неизвестно

- схемы JSON-ответов (пагинация, поля треков/альбомов, обёртки);
- точное время жизни `x-core-token` (ошибка `TIMESTAMP_EXPIRED` есть,
  границы окна не измерены);
- формат `red` при `x-accept-red: 1`;
- POST/PUT/DELETE и методы записи;
- другие локали (`en`, `kz`, ...) и их хосты;
- обязательность `ldi`/`x-theme`/`x-app-version` для каждого эндпоинта.

Когда появятся рабочие `libs` и свежий токен, снятые ответы можно добавлять
сюда же, в таблицу эндпоинтов.
