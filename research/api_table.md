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
