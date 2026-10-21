# Ответы API: схемы

Снято с `x-accept-red: 0`, метод `GET`, пустые query и body (кроме отмеченных).
Файлы — `http-request-dumps/*.response.txt`. Схемы получены `tools/schema.py`
и вручную сведены по типам объектов.

---

## 1. Общие объекты

### Track (трек)

Общий каркас, дальше расходится по платформам.

| Поле | Тип | Есть всегда | Комментарий |
|---|---|---|---|
| `songId` | string | да | id в терминах платформы |
| `platform` | string | да | `spotify` \| `soundcloud` |
| `title` | string | да | |
| `artistsDisplayedName` | string | да | одна строка, не массив |
| `coverUrl` | string | да | у Spotify — прокси `cdn.laneapi.com/spotify/…` |
| `duration` | string | да | `"2:11"` |
| `artistAvatars` | string[] | да | |
| `explicit` | bool | нет | встречается только `true`, отсутствует = false |
| `initTimestamp` | int | нет | epoch **мс**, есть у части Spotify-треков (9/16) |
| `genre` | string | нет | у SoundCloud, встречается литерал `"null"` |

**Spotify:** `spData`

```json
"spData": {
  "album": "1rfSlBGQbwb6TCoG4HqvNe",
  "artists": ["1K7Bggsg4Zgv6ecC0iQhsl"],
  "mp4128FileId": "3ea610f0a83c1e0236f9b2067316bddf30ad87f9"
}
```

`mp4128FileId` — 40 hex-символов, не URL. Похоже на «хэндл» файла, который
потом обменивается на аудио отдельным запросом.

**SoundCloud:** `scData`

```json
"scData": {
  "userId": "1675149233",
  "urn": "soundcloud:tracks:2402055060",
  "streamUrl": "https://api-v2.soundcloud.com/media/soundcloud:tracks:2402055060/…/stream/progressive",
  "s3Key": "track/soundcloud/2402055060.mp3"
}
```

`streamUrl` — прямой URL на SoundCloud, **без query-параметров** (никакой
подписи/срока в самом URL нет). `s3Key` — ключ в хранилище Lane.

### Playlist (плейлист)

```json
{
  "playlistId": "lane_likes",
  "playlistName": "Любимые треки",
  "playlistImageUrl": "https://cdn.laneapi.com/static/img/favorites…",
  "playlistTracksIds": ["…", "…"],
  "playlistDescription": "…",     // опционально
  "creatorLid": "spotify",        // опционально
  "platform": "spotify",          // опционально
  "lastPrefetchTime": 1791742571325, // опционально, epoch мс
  "tracksCount": 49               // встречается в /user/recent
}
```

### Album

```json
{
  "id": "1n74b3FUpRhNHVBrCSK4xL",
  "name": "Danganronpa V3: Killing Harmony Original Soundtrack",
  "platform": "spotify",
  "type": "album",
  "coverUrl": "https://cdn.laneapi.com/spotify/i.scdn.co/image/…",
  "year": "2017-02-24",
  "artists": ["…"],
  "artistsDisplayedName": "Masafumi Takada",
  "lastUpdated": 1792513524991
}
```

### Artist

```json
{
  "id": "1032037858",
  "name": "NAPAS911",
  "platform": "soundcloud",
  "avatarUrl": "https://i1.sndcdn.com/avatars-…",
  "headerUrl": "https://i1.sndcdn.com/visuals-…",
  "lastUpdated": 1792481983692,
  "relatedArtists": [{"id":"…","name":"…","avatarUrl":"…","platform":"soundcloud"}],
  "description": "…",   // опционально
  "biography": "…"      // опционально, бывает HTML
}
```

### Пагинация

```json
{"items": [...], "totalItems": 170, "page": 1, "pageSize": 20, "totalPages": 9}
```

---

## 2. Эндпоинты

### `GET /user/playlists` → `Playlist[]`

```markdown
- root: array<object>
  - playlistId: string
  - playlistName: string
  - playlistImageUrl: string
  - playlistTracksIds: array<string>
  - playlistDescription: string (optional)
  - creatorLid: string (optional)
  - platform: string (optional)
  - lastPrefetchTime: int (optional)
```

### `GET /playlist/{id}` → `Playlist`

```markdown
- root: object
  - playlistId: string          e.g. "lane_likes"
  - playlistName: string
  - playlistImageUrl: string
  - playlistTracksIds: array<string>   (у lane_likes — 170 элементов)
```

### `GET /playlist/{id}/tracks?page=1&pageSize=50` → `Page<Track>`

```markdown
- root: object
  - items: array<Track>
  - totalItems: int   e.g. 170
  - page: int         e.g. 1
  - pageSize: int     e.g. 20
  - totalPages: int   e.g. 9
```

Замечание: при запросе `pageSize=50` сервер вернул `pageSize: 20` — размер
страницы ограничивается на его стороне.

### `GET /user/artists` → `Artist[]`

```markdown
- root: array<object>
  - id: string
  - name: string
  - platform: string
  - avatarUrl: string
  - headerUrl: string
  - lastUpdated: int
  - relatedArtists: array<object>
    - id, name, avatarUrl: string
    - platform: string (optional)
  - description: string (optional)
  - biography: string (optional)
```

### `GET /user/albums` → `Album[]`

```markdown
- root: array<object>
  - id, name, platform, type: string
  - coverUrl: string
  - year: string        e.g. "2017-02-24"
  - artists: array<string>
  - artistsDisplayedName: string
  - lastUpdated: int
```

### `GET /user/recent` → лента

```markdown
- root: object
  - id: string      e.g. "recently_played"
  - title: string   e.g. "Недавно прослушано"
  - type: string    e.g. "HISTORY"
  - content: array<object>
    - type: string  e.g. "playlist_item"
    - id: string
    - playlist: Playlist (см. выше; бывает tracksCount)
```

### `GET /user/search/history` → `HistoryItem[]` (полиморфный)

Дискриминатор — `type`, значения — имена Java-классов:

| `type` | Кол-во в дампе | Поля `data` |
|---|---|---|
| `…LaneHistoryItem.ArtistItem` | 14 | `id, name, platform, avatarUrl, headerUrl, lastUpdated, relatedArtists[]`, опц. `biography`, `description` |
| `…LaneHistoryItem.TrackItem` | 14 | объект `Track` целиком |
| `…LaneHistoryItem.AlbumItem` | 2 | объект `Album` целиком |

```json
{"type":"com.skiy.lane.data.model.LaneHistoryItem.ArtistItem","data":{ … }}
```

### `GET /platforms/v2/hints?q=…`

С пустым `q` возвращает **не JSON**, а текст:

| Query | Ответ |
|---|---|
| без `q` | `Missing query parameter` (23 байта) |
| `q=` (пустой) | `Empty query` (11 байт) |

С непустым `q` — обычный JSON:

```markdown
- root: object
  - bestTextMatches: array<string>     e.g. ["idk", "idk sekairotten", …]
  - token: string                       base64url, см. ниже
  - entities: array<object>
    - type: string                      …SmartSearchEntity.TrackItem | .ArtistItem
    - track: Track    (optional, для TrackItem)
    - artist: Artist  (optional, для ArtistItem)
```

`entities` полиморфны по `type` (полное имя класса, как в истории поиска):
`TrackItem` → поле `track`, `ArtistItem` → поле `artist`.

**`token`** — base64url, внутри JSON + 40 байт бинарного хвоста:

```
{"userId":"e7ffca54a1b74572b22107ad2004bcaf","query":"idk","timestamp":1792541300911}
<40 байт>
```

То есть это continuation-токен, привязанный к пользователю и запросу
(`userId`, `query`, `timestamp`) плюс, судя по длине, подпись/MAC. Для клиента
он непрозрачен: его нужно просто возвращать как есть.

### `GET /events`

Стабильно уходит в таймаут (и у клиента, и при съёмке). Причина не выяснена;
возможно, сервер ждёт тело/заголовки `x-connection-quality`/`x-is-vpn` или это
долгий long-poll.

### `POST /user/tracks?prefetch=0` → `Track[]`

На `GET` отвечает **405 с пустым телом**. Рабочий запрос:

```
POST /user/tracks?prefetch=0
content-type: application/json; charset=UTF8

{"trackIds":["2lEbe9evbwJ1unoQAb0uPk"]}
```

Причём тело **шифруется**: на проводе лежит `RC4*(KEY‖nonce‖ts, json)`, то есть
`out.payload` из `build()` (см. `ENDPOINTS.md` §4.1). Сервер расшифровывает его
и возвращает массив `Track` — тех же объектов, что и в `/playlist/{id}/tracks`,
уже с `spData`/`scData`.

Проверено: `{"trackIds":[…]}` → `200`; `[…]`, `{"ids":[…]}`, `{"songIds":[…]}`
и `{"tracks":[…]}` → `400 INVALID_TRACK_IDS_BODY`.

---

## 3. Хэндлы и время

Что подтверждено:

- отдельного поля «ссылка» с `?token=…`/`expires=…` нигде нет — все URL в
  ответах статические (проверено по всем ответам: query-параметров у ссылок 0);
- `mp4128FileId` (Spotify) и `s3Key` (SoundCloud) — это идентификаторы файла,
  а не готовые ссылки;
- времена — epoch **миллисекунды**: `lastUpdated`, `lastPrefetchTime`,
  `initTimestamp`.

Что пока не измерено:

- сколько живёт `mp4128FileId` и что происходит после истечения (ошибка или
  новый id);
- у части Spotify-треков `initTimestamp` есть, у части нет — вероятно, это
  время генерации хэндла, но подтвердить можно только повторным снятием
  того же трека через несколько часов;
- `streamUrl` у SoundCloud выглядит статическим, но, возможно, начинает
  отдавать 403 спустя время — тоже проверяется повторным запросом.

---

## 4. Замеченные странности

1. `genre` у SoundCloud иногда — **строка `"null"`**, а не JSON-null.
2. `explicit` приходит только когда `true`; отсутствие поля = false.
3. Разные соглашения для дискриминатора: в истории — полное имя класса
   (`com.skiy.lane.data.model.LaneHistoryItem.TrackItem`), в `/user/recent` —
   `playlist_item`.
4. `pageSize` в ответе может быть меньше запрошенного (20 против 50).
5. `year` в альбоме — полная дата `"2017-02-24"`, а не год.
6. Картинки Spotify проксируются через `cdn.laneapi.com/spotify/…`,
   SoundCloud — напрямую с `i1.sndcdn.com`.

---

## 5. Чего не хватает

- остальных `POST/PUT/DELETE` (кроме `/user/tracks`);
- замера TTL хэндлов;
- формата `red` (`x-accept-red: 1`).
