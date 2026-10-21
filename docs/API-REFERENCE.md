# Lane HTTP API — полный справочник

Источник: декомпилированные Retrofit-интерфейсы APK 1.4.7
(`com.skiy.lane.data.remote.api.*`), см. [research/apk](../research/apk).
Таблица сгенерирована из сигнатур, поэтому покрывает **все** объявленные
эндпоинты, а не только те, что мы перехватили.

Обозначения:

- ✅ — проверено вживую (дампы из `http-request-dumps/` или собственные запросы);
- 🟡 — путь и параметры из APK, схема ответа не снята;
- ⚪ — известна только сигнатура: тело запроса — модель, поля не разобраны.

## Общие правила

Все запросы к `https://ru.laneapi.com` подписываются одинаково:

1. `query` канонизируется: имена параметров дедуплицируются, сортируются,
   склеиваются как `name=value` через `&` (см. `ALGORITHM.md` §5.4).
2. Тело POST **шифруется**: `RC4*(KEY‖nonce‖ts, json)`, и в `msg1` попадает
   тот же шифртекст. Отправляется `out.payload`.
3. Ответ с `X-Core-Red: 1` расшифровывается `RC4*(KEY‖X-Resp-Nonce)` (+gzip).
4. **Исключение — multipart** (`/user/upload/photo`): интерцептор видит
   `multipart` и не трогает тело; подпись считается по пустому `body`.

## Все эндпоинты

Пути без ведущего `/` в аннотации (`events`, `user/follow/…`) Retrofit
разрешает относительно базы — фактически это те же `/events`, `/user/follow/…`.

| Метод | Путь | Параметры | Ответ |
|---|---|---|---|
| GET | `/account` | header `DL` | `User` |
| POST | `/artists/resolve` | body `ResolveArtistsRequest` | `List<ResolveArtistResponse>` |
| POST | `/create-playlist` | body `CreatePlaylist` | `PlaylistCreationResponse` |
| GET | `/createUserOrLogin` | query `idToken` | `ResponseBody` |
| GET | `/delete-playlist` | query `playlistId` | `ResponseBody` |
| POST | `/edit-playlist` | body `EditPlaylist` | `ResponseBody` |
| GET | `/feed/home` | — | `UserHomeFeed` |
| GET | `/onboarding/artists` | query `page`, query `pageSize`, query `q` | `Page<OnboardingArtistDto>` |
| POST | `/onboarding/finish` | body `SubmitOnboardingRequest` | `Response<ResponseBody>` |
| POST | `/p2p/callback` | body `P2PCallbackRequest` | `Unit` |
| POST | `/payment/cancel-subscription` | — | `CancelSubscriptionResponse` |
| GET | `/payment/pricing` | — | `UserPricing` |
| GET | `/platforms/album` | query `albumId` | `LaneAlbumItem` |
| GET | `/platforms/artist` | query `artistId` | `LaneArtistItem` |
| GET | `/platforms/artist/rip/candles-count` | query `artistId` | `Long` |
| GET | `/platforms/recommendations` | query `trackId`, query `platform` | `LanePlaylistItem` |
| GET | `/platforms/search` | query `q`, query `platform`, query `ver` | `LaneCombinedSearchResponse` |
| POST | `/platforms/search/click` | body `SearchClickEvent` | `ResponseBody` |
| GET | `/platforms/v2/hints` | query `q` | `SmartHintsResponse` |
| POST | `/playlist/invite/respond` | body `RespondToInvitationRequest` | `Response<ResponseBody>` |
| POST | `/playlist/reorder` | body `ReorderPlaylistRequest` | `Response<Unit>` |
| GET | `/playlist/{playlistId}` | path `playlistId`, query `platform` | `LanePlaylistItem` |
| GET | `/playlist/{playlistId}/cached-streams` | path `playlistId` | `CachedPlaylistStreamsResponse` |
| GET | `/playlist/{playlistId}/collaborators` | path `playlistId` | `List<UserInfo>` |
| DELETE | `/playlist/{playlistId}/collaborators/{targetUserId}` | path `playlistId`, path `targetUserId` | `Response<ResponseBody>` |
| POST | `/playlist/{playlistId}/invite` | path `playlistId`, body `InviteUsersRequest` | `BulkInvitationResponse` |
| GET | `/playlist/{playlistId}/tracks` | path `playlistId`, query `page`, query `pageSize` | `Page<TrackData>` |
| GET | `/share/create` | query `shareElementId`, query `shareType` | `LaneShareItem` |
| GET | `/track/download` | query `trackId`, query `streamQuality` | `TrackStreamingResult` |
| GET | `/track/effect` | query `effect`, query `trackId` | `TrackStreamingResult` |
| POST | `/track/log` | query `token` | `ResponseBody` |
| GET | `/track/stats` | query `trackId` | `TrackStats` |
| GET | `/track/stream` | query `trackId`, query `refId`, query `streamQuality` | `TrackStreamingResult` |
| GET | `/user-info` | query `laneId` | `UserInfo` |
| GET | `/user/albums` | — | `List<LaneAlbumItem>` |
| GET | `/user/artists` | — | `List<LaneArtistItem>` |
| GET | `/user/check-user-name` | query `username` | `Boolean` |
| POST | `/user/edit` | body `EditProfileData` | `Unit` |
| GET | `/user/friends` | query `page`, query `pageSize`, query `q` | `Page<UserInfo>` |
| GET | `/user/friends/presence` | — | `List<FriendPresenceDto>` |
| GET | `/user/history-bump-item` | query `item` | `ResponseBody` |
| GET | `/user/history-delete-item` | query `item` | `ResponseBody` |
| GET | `/user/playlist/add` | query `playlistId` | `ResponseBody` |
| POST | `/user/playlist/add-tracks` | query `playlistId`, body `List<String>` | `Unit` |
| GET | `/user/playlist/remove-track` | query `playlistId`, query `trackId` | `ResponseBody` |
| GET | `/user/playlists` | — | `List<LanePlaylistItem>` |
| POST | `/user/presence` | body `UpdatePresenceRequest` | `Response<ResponseBody>` |
| GET | `/user/recent` | — | `RecommendationSection` |
| GET | `/user/search/history` | — | `List<LaneHistoryItem>` |
| POST | `/user/settings/privacy` | body `UpdatePrivacyRequest` | `Response<ResponseBody>` |
| POST | `/user/subscribe/artist/{id}` | path `id` | `ResponseBody` |
| POST | `/user/tracks` | query `prefetch`, body `TrackIds` | `List<TrackData>` |
| DELETE | `/user/unsubscribe/artist/{id}` | path `id` | `ResponseBody` |
| POST | `/user/upload/photo` | query `type`, multipart | `ImageUploadResponse` |
| GET | `/users/search` | query `q` | `List<UserInfo>` |
| GET | `/v1/share/get` | query `id` | `LaneOpenShareItem` |
| POST | `artist/{artistId}/candle` | path `artistId`, body `PlaceCandleRequest` | `ArtistCandle` |
| GET | `artist/{artistId}/candles` | path `artistId`, query `page`, query `pageSize` | `Page<ArtistCandle>` |
| GET | `badges/definitions` | — | `List<BadgeDefinition>` |
| POST | `comment/track` | body `CreateTrackComment` | `CreatedCommentResponse` |
| DELETE | `comment/{commentId}/like` | path `commentId` | `ResponseBody` |
| POST | `comment/{commentId}/like` | path `commentId` | `ResponseBody` |
| GET | `comment/{commentId}/replies` | path `commentId`, query `page`, query `pageSize` | `Page<LaneTrackComment>` |
| POST | `comment/{commentId}/reply` | path `commentId`, body `CreateReply` | `CreatedReplyResponse` |
| GET | `events` | header `X-Connection-Quality`, header `X-Is-Vpn` | `List<ServerEvent>` |
| GET | `import/telegram/finish` | — | `LanePlaylistItem` |
| GET | `import/telegram/start` | — | `TelegramImportCode` |
| GET | `notifications` | query `page`, query `pageSize`, query `filter` | `Page<AppNotificationResponse>` |
| POST | `notifications/read-all` | — | `Unit` |
| GET | `notifications/unread-count` | — | `UnreadCountResponse` |
| POST | `notifications/{id}/read` | path `id` | `Unit` |
| POST | `playlist/{playlistId}/visibility` | path `playlistId`, body `SetPlaylistVisibilityRequest` | `Unit` |
| GET | `track/{trackId}/comments` | path `trackId`, query `page`, query `pageSize`, query `sortBy`, query `highlightCommentId` | `Page<LaneTrackComment>` |
| GET | `track/{trackId}/lyrics` | path `trackId` | `TrackLyrics` |
| POST | `user/badge/equip` | body `EquipBadgeRequest` | `Unit` |
| POST | `user/fcm-token` | body `UpdateFcmRequest` | `Response<ResponseBody>` |
| POST | `user/follow/{id}` | path `id` | `Unit` |
| GET | `user/followers` | query `laneId`, query `page`, query `pageSize` | `Page<UserInfo>` |
| GET | `user/following` | query `laneId`, query `page`, query `pageSize` | `Page<UserInfo>` |
| GET | `user/import/preview` | query `platform`, query `spotifyBearerToken`, query `spotifyClientToken`, query `spotifyPlaylistId`, query `soundcloudPlaylistId`, query `yandexPlaylistId`, query `soundcloudProfileUrl` | `LanePlaylistItem` |
| POST | `user/status` | body `SetStatusTrackRequest` | `Unit` |
| POST | `user/subscribe/album/{id}` | path `id` | `Unit` |
| DELETE | `user/unfollow/{id}` | path `id` | `Unit` |
| DELETE | `user/unsubscribe/album/{id}` | path `id` | `Unit` |

## Telegram-авторизация

Полная спецификация флоу — в [AUTH.md](./AUTH.md).

Входа по логину/паролю нет: приложение открывает бота и ждёт, пока тот
подтвердит сессию. Найдено в `LoginScreenKt` и `AuthRepositoryImpl`:

```
1) androidId = Settings.Secure.getString(cr, "android_id")   // 16 hex-символов
2) open https://t.me/lane_music_bot?start=auth<androidId>
3) бот связывает Telegram-аккаунт с этим androidId на сервере
4) приложение опрашивает GET {base}/auth/<androidId>
   каждые 3 с, не более 20 раз (~60 с)
5) 200 + JSON → LaneTokenResponse { token, isFirstAuth, user }
```

Особенности:

- `/auth/{id}` не объявлен в Retrofit — вызывается сырым OkHttp, поэтому его нет
  в таблице выше. Подписывается он как все: интерцептор видит тот же хост.
- `Authorization` на этом шаге ещё нет (токена нет), `AuthInterceptor` просто
  не добавляет заголовок.
- Ответ — обычный (не `red`) JSON; при ошибке/неготовности сервер отвечает
  не-200, и клиент продолжает опрос.
- `isFirstAuth = true` → приложение ведёт на онбординг (`/onboarding/artists`,
  `/onboarding/finish`).
- `<androidId>` — произвольная строка из `A-Za-z0-9_-` до 60 символов; формат
  не проверяется (подтверждено живым логином с выдуманным id).

В CLI:

```powershell
laned auth-url <android_id>                      # печатает ссылку на бота
laned auth-wait <android_id> --out token.json    # опрашивает /auth/<id>
```

## Загрузка файлов

Единственная multipart-ручка — аватар/обложка:

```
POST /user/upload/photo?type=<type>
Content-Type: multipart/form-data; boundary=…

<part name="file"?> — картинка
→ ImageUploadResponse
```

Что важно:

- тело **не шифруется** и `field` пустой (см. «Общие правила»);
- имя multipart-части и допустимые `type` из APK не видны — нужно проверить
  запросом (вероятно `type=avatar` / `type=cover`);
- ответ `ImageUploadResponse` содержит URL, который потом передаётся в
  `POST /user/edit` (профиль) или `/edit-playlist`.

Своих загрузок треков в API нет: треки приходят из Spotify/SoundCloud по
`trackId`, локальные файлы приложение не грузит.

## Проверено вживую

| Эндпоинт | Чем |
|---|---|
| `GET /user/playlists`, `/user/albums`, `/user/artists`, `/user/recent` | дампы + наши запросы |
| `GET /user/search/history` | дамп + разбор `INVALID_SIGNATURE` |
| `POST /user/tracks` | дамп + собственный запрос (`{"trackIds":[…]}`) |
| `GET /playlist/{id}`, `GET /playlist/{id}/tracks` | дампы |
| `GET /platforms/search`, `/platforms/v2/hints` | дампы (с оговоркой по `q`) |
| `GET /events` | дамп |
| `GET /track/stream` | собственный запрос, 200 |

Всё остальное — 🟡/⚪: путь и параметры известны, тела запросов и схемы ответов
надо снимать по одному.

## Чего не хватает

1. Схемы тел запросов (`CreatePlaylist`, `EditPlaylist`, `EditProfileData`,
   `TrackIds`, `InviteUsersRequest`, `PlaceCandleRequest`, …) — модели есть в
   APK, но их поля ещё не выписаны.
2. Схемы ответов для `User`, `UserInfo`, `UserPricing`, `UserHomeFeed`,
   `SmartHintsResponse`, `Notifications`, `Comments`.
3. Точные значения `type` для загрузки фото и имя multipart-части.
4. `p2p/callback`, `import/telegram/*`, `user/import/preview` — сценарии
   внешних интеграций, вживую не проверялись.

## Как дёргать новое

CLI уже умеет произвольные подписанные запросы:

```powershell
laned get /payment/pricing
laned get /track/stats trackId=2lEbe9evbwJ1unoQAb0uPk
laned post /user/playlist/add-tracks playlistId=lane_likes --body '["2lEbe9evbwJ1unoQAb0uPk"]'
```

`get`/`post` канонизируют query и шифруют тело сами.
