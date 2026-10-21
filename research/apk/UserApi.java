package com.skiy.lane.data.remote.api;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.upstream.CmcdData;
import coil3.util.UtilsKt;
import com.skiy.lane.domain.model.AppNotificationResponse;
import com.skiy.lane.domain.model.BadgeDefinition;
import com.skiy.lane.domain.model.BulkInvitationResponse;
import com.skiy.lane.domain.model.CachedPlaylistStreamsResponse;
import com.skiy.lane.domain.model.CancelSubscriptionResponse;
import com.skiy.lane.domain.model.CreatePlaylist;
import com.skiy.lane.domain.model.EditPlaylist;
import com.skiy.lane.domain.model.EditProfileData;
import com.skiy.lane.domain.model.EquipBadgeRequest;
import com.skiy.lane.domain.model.FriendPresenceDto;
import com.skiy.lane.domain.model.ImageUploadResponse;
import com.skiy.lane.domain.model.InviteUsersRequest;
import com.skiy.lane.domain.model.LaneAlbumItem;
import com.skiy.lane.domain.model.LaneArtistItem;
import com.skiy.lane.domain.model.LaneHistoryItem;
import com.skiy.lane.domain.model.LanePlaylistItem;
import com.skiy.lane.domain.model.OnboardingArtistDto;
import com.skiy.lane.domain.model.P2PCallbackRequest;
import com.skiy.lane.domain.model.PaginatedResult;
import com.skiy.lane.domain.model.PlaylistCreationResponse;
import com.skiy.lane.domain.model.RecommendationSection;
import com.skiy.lane.domain.model.ReorderPlaylistRequest;
import com.skiy.lane.domain.model.ResolveArtistResponse;
import com.skiy.lane.domain.model.ResolveArtistsRequest;
import com.skiy.lane.domain.model.RespondToInvitationRequest;
import com.skiy.lane.domain.model.ServerEvent;
import com.skiy.lane.domain.model.SetPlaylistVisibilityRequest;
import com.skiy.lane.domain.model.SetStatusTrackRequest;
import com.skiy.lane.domain.model.SubmitOnboardingRequest;
import com.skiy.lane.domain.model.TelegramImportCode;
import com.skiy.lane.domain.model.TrackData;
import com.skiy.lane.domain.model.TrackIds;
import com.skiy.lane.domain.model.UnreadCountResponse;
import com.skiy.lane.domain.model.UpdateFcmRequest;
import com.skiy.lane.domain.model.UpdatePresenceRequest;
import com.skiy.lane.domain.model.UpdatePrivacyRequest;
import com.skiy.lane.domain.model.User;
import com.skiy.lane.domain.model.UserHomeFeed;
import com.skiy.lane.domain.model.UserInfo;
import com.skiy.lane.domain.model.UserPricing;
import java.util.List;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes.dex */
@Metadata(d1 = {"\u0000ä\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J&\u0010\u0006\u001a\u00020\u00052\b\b\u0001\u0010\u0003\u001a\u00020\u00022\n\b\u0003\u0010\u0004\u001a\u0004\u0018\u00010\u0002H§@¢\u0006\u0004\b\u0006\u0010\u0007J\u0016\u0010\n\u001a\b\u0012\u0004\u0012\u00020\t0\bH§@¢\u0006\u0004\b\n\u0010\u000bJ\u0016\u0010\r\u001a\b\u0012\u0004\u0012\u00020\f0\bH§@¢\u0006\u0004\b\r\u0010\u000bJ\u001a\u0010\u0010\u001a\u00020\u000f2\b\b\u0001\u0010\u000e\u001a\u00020\u0002H§@¢\u0006\u0004\b\u0010\u0010\u0011J\u001a\u0010\u0014\u001a\u00020\u00132\b\b\u0001\u0010\u0012\u001a\u00020\u0002H§@¢\u0006\u0004\b\u0014\u0010\u0011J\u001a\u0010\u0018\u001a\u00020\u00172\b\b\u0001\u0010\u0016\u001a\u00020\u0015H§@¢\u0006\u0004\b\u0018\u0010\u0019J\u001a\u0010\u001a\u001a\u00020\u000f2\b\b\u0001\u0010\u0003\u001a\u00020\u0002H§@¢\u0006\u0004\b\u001a\u0010\u0011J*\u0010\u001c\u001a\u00020\u00172\b\b\u0001\u0010\u0003\u001a\u00020\u00022\u000e\b\u0001\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00020\bH§@¢\u0006\u0004\b\u001c\u0010\u001dJ$\u0010\u001e\u001a\u00020\u000f2\b\b\u0001\u0010\u0003\u001a\u00020\u00022\b\b\u0001\u0010\u000e\u001a\u00020\u0002H§@¢\u0006\u0004\b\u001e\u0010\u0007J\u001a\u0010\"\u001a\u00020!2\b\b\u0001\u0010 \u001a\u00020\u001fH§@¢\u0006\u0004\b\"\u0010#J\u001a\u0010&\u001a\u00020\u000f2\b\b\u0001\u0010%\u001a\u00020$H§@¢\u0006\u0004\b&\u0010'J\u001a\u0010(\u001a\u00020\u000f2\b\b\u0001\u0010\u0003\u001a\u00020\u0002H§@¢\u0006\u0004\b(\u0010\u0011J*\u0010-\u001a\b\u0012\u0004\u0012\u00020,0\b2\b\b\u0001\u0010*\u001a\u00020)2\b\b\u0001\u0010+\u001a\u00020\u0002H§@¢\u0006\u0004\b-\u0010.J\u0016\u0010/\u001a\b\u0012\u0004\u0012\u00020\u00050\bH§@¢\u0006\u0004\b/\u0010\u000bJ\u001a\u00102\u001a\u0002012\b\b\u0003\u00100\u001a\u00020\u0002H§@¢\u0006\u0004\b2\u0010\u0011J\u001a\u00105\u001a\u0002042\b\b\u0001\u00103\u001a\u00020\u0002H§@¢\u0006\u0004\b5\u0010\u0011J\u001a\u00107\u001a\u00020\u000f2\b\b\u0001\u00106\u001a\u00020\u0002H§@¢\u0006\u0004\b7\u0010\u0011J$\u0010<\u001a\u00020;2\b\b\u0001\u00108\u001a\u00020\u00022\b\b\u0001\u0010:\u001a\u000209H§@¢\u0006\u0004\b<\u0010=J\u001a\u0010?\u001a\u00020\u000f2\b\b\u0001\u0010>\u001a\u00020\u0002H§@¢\u0006\u0004\b?\u0010\u0011J\u001a\u0010@\u001a\u00020\u000f2\b\b\u0001\u0010>\u001a\u00020\u0002H§@¢\u0006\u0004\b@\u0010\u0011J\u001a\u0010C\u001a\u00020\u00172\b\b\u0001\u0010B\u001a\u00020AH§@¢\u0006\u0004\bC\u0010DJ\u001a\u0010F\u001a\u00020\u00172\b\b\u0001\u0010E\u001a\u00020\u0002H§@¢\u0006\u0004\bF\u0010\u0011J\u001a\u0010G\u001a\u00020\u00172\b\b\u0001\u0010E\u001a\u00020\u0002H§@¢\u0006\u0004\bG\u0010\u0011J4\u0010L\u001a\b\u0012\u0004\u0012\u0002040K2\b\b\u0001\u0010E\u001a\u00020\u00022\b\b\u0001\u0010I\u001a\u00020H2\b\b\u0001\u0010J\u001a\u00020HH§@¢\u0006\u0004\bL\u0010MJ4\u0010N\u001a\b\u0012\u0004\u0012\u0002040K2\b\b\u0001\u0010E\u001a\u00020\u00022\b\b\u0001\u0010I\u001a\u00020H2\b\b\u0001\u0010J\u001a\u00020HH§@¢\u0006\u0004\bN\u0010MJb\u0010U\u001a\u00020\u00052\b\b\u0001\u0010\u0004\u001a\u00020\u00022\n\b\u0003\u0010O\u001a\u0004\u0018\u00010\u00022\n\b\u0003\u0010P\u001a\u0004\u0018\u00010\u00022\n\b\u0003\u0010Q\u001a\u0004\u0018\u00010\u00022\n\b\u0003\u0010R\u001a\u0004\u0018\u00010\u00022\n\b\u0003\u0010S\u001a\u0004\u0018\u00010\u00022\n\b\u0003\u0010T\u001a\u0004\u0018\u00010\u0002H§@¢\u0006\u0004\bU\u0010VJ\u0010\u0010X\u001a\u00020WH§@¢\u0006\u0004\bX\u0010\u000bJ\u0010\u0010Y\u001a\u00020\u0005H§@¢\u0006\u0004\bY\u0010\u000bJ\u001a\u0010[\u001a\u00020\u00172\b\b\u0001\u0010Z\u001a\u00020\u0002H§@¢\u0006\u0004\b[\u0010\u0011J\u001a\u0010\\\u001a\u00020\u00172\b\b\u0001\u0010Z\u001a\u00020\u0002H§@¢\u0006\u0004\b\\\u0010\u0011J*\u0010`\u001a\b\u0012\u0004\u0012\u00020_0\b2\b\b\u0001\u0010]\u001a\u00020\u00022\b\b\u0001\u0010^\u001a\u00020\u0013H§@¢\u0006\u0004\b`\u0010aJ \u0010d\u001a\b\u0012\u0004\u0012\u00020\u000f0c2\b\b\u0001\u0010B\u001a\u00020bH§@¢\u0006\u0004\bd\u0010eJ$\u0010g\u001a\u00020\u00172\b\b\u0001\u0010\u0003\u001a\u00020\u00022\b\b\u0001\u0010B\u001a\u00020fH§@¢\u0006\u0004\bg\u0010hJ\u001a\u0010j\u001a\u00020\u00172\b\b\u0001\u0010B\u001a\u00020iH§@¢\u0006\u0004\bj\u0010kJ\u0016\u0010m\u001a\b\u0012\u0004\u0012\u00020l0\bH§@¢\u0006\u0004\bm\u0010\u000bJ6\u0010p\u001a\b\u0012\u0004\u0012\u00020o0K2\b\b\u0001\u0010I\u001a\u00020H2\b\b\u0001\u0010J\u001a\u00020H2\n\b\u0001\u0010n\u001a\u0004\u0018\u00010\u0002H§@¢\u0006\u0004\bp\u0010qJ\u001a\u0010s\u001a\u00020\u00172\b\b\u0001\u0010r\u001a\u00020\u0002H§@¢\u0006\u0004\bs\u0010\u0011J\u0010\u0010t\u001a\u00020\u0017H§@¢\u0006\u0004\bt\u0010\u000bJ\u0010\u0010v\u001a\u00020uH§@¢\u0006\u0004\bv\u0010\u000bJ$\u0010y\u001a\u00020x2\b\b\u0001\u0010\u0003\u001a\u00020\u00022\b\b\u0001\u0010B\u001a\u00020wH§@¢\u0006\u0004\by\u0010zJ \u0010|\u001a\b\u0012\u0004\u0012\u00020\u000f0c2\b\b\u0001\u0010B\u001a\u00020{H§@¢\u0006\u0004\b|\u0010}J6\u0010\u007f\u001a\b\u0012\u0004\u0012\u0002040K2\b\b\u0001\u0010I\u001a\u00020H2\b\b\u0001\u0010J\u001a\u00020H2\n\b\u0003\u0010~\u001a\u0004\u0018\u00010\u0002H§@¢\u0006\u0004\b\u007f\u0010qJ\"\u0010\u0080\u0001\u001a\b\u0012\u0004\u0012\u0002040\b2\b\b\u0001\u0010\u0003\u001a\u00020\u0002H§@¢\u0006\u0005\b\u0080\u0001\u0010\u0011J-\u0010\u0082\u0001\u001a\b\u0012\u0004\u0012\u00020\u000f0c2\b\b\u0001\u0010\u0003\u001a\u00020\u00022\t\b\u0001\u0010\u0081\u0001\u001a\u00020\u0002H§@¢\u0006\u0005\b\u0082\u0001\u0010\u0007J\u0013\u0010\u0084\u0001\u001a\u00030\u0083\u0001H§@¢\u0006\u0005\b\u0084\u0001\u0010\u000bJ$\u0010\u0086\u0001\u001a\b\u0012\u0004\u0012\u00020\u000f0c2\t\b\u0001\u0010B\u001a\u00030\u0085\u0001H§@¢\u0006\u0006\b\u0086\u0001\u0010\u0087\u0001J\u0013\u0010\u0089\u0001\u001a\u00030\u0088\u0001H§@¢\u0006\u0005\b\u0089\u0001\u0010\u000bJ\"\u0010\u008a\u0001\u001a\b\u0012\u0004\u0012\u0002040\b2\b\b\u0001\u0010~\u001a\u00020\u0002H§@¢\u0006\u0005\b\u008a\u0001\u0010\u0011J\u0013\u0010\u008c\u0001\u001a\u00030\u008b\u0001H§@¢\u0006\u0005\b\u008c\u0001\u0010\u000bJ%\u0010\u008f\u0001\u001a\t\u0012\u0005\u0012\u00030\u008e\u00010\b2\t\b\u0001\u0010B\u001a\u00030\u008d\u0001H§@¢\u0006\u0006\b\u008f\u0001\u0010\u0090\u0001J\u0013\u0010\u0092\u0001\u001a\u00030\u0091\u0001H§@¢\u0006\u0005\b\u0092\u0001\u0010\u000bJ\u001e\u0010\u0094\u0001\u001a\u00020\u00172\t\b\u0001\u0010B\u001a\u00030\u0093\u0001H§@¢\u0006\u0006\b\u0094\u0001\u0010\u0095\u0001J9\u0010\u0097\u0001\u001a\t\u0012\u0005\u0012\u00030\u0096\u00010K2\b\b\u0001\u0010I\u001a\u00020H2\b\b\u0001\u0010J\u001a\u00020H2\n\b\u0003\u0010~\u001a\u0004\u0018\u00010\u0002H§@¢\u0006\u0005\b\u0097\u0001\u0010qJ$\u0010\u0099\u0001\u001a\b\u0012\u0004\u0012\u00020\u000f0c2\t\b\u0001\u0010B\u001a\u00030\u0098\u0001H§@¢\u0006\u0006\b\u0099\u0001\u0010\u009a\u0001J\u001d\u0010\u009c\u0001\u001a\u00030\u009b\u00012\b\b\u0001\u0010\u0003\u001a\u00020\u0002H§@¢\u0006\u0005\b\u009c\u0001\u0010\u0011J$\u0010\u009e\u0001\u001a\b\u0012\u0004\u0012\u00020\u00170c2\t\b\u0001\u0010B\u001a\u00030\u009d\u0001H§@¢\u0006\u0006\b\u009e\u0001\u0010\u009f\u0001J\u0019\u0010¡\u0001\u001a\t\u0012\u0005\u0012\u00030 \u00010\bH§@¢\u0006\u0005\b¡\u0001\u0010\u000bJ\u001d\u0010£\u0001\u001a\u00020\u000f2\t\b\u0001\u0010¢\u0001\u001a\u00020\u0002H§@¢\u0006\u0005\b£\u0001\u0010\u0011J6\u0010¤\u0001\u001a\b\u0012\u0004\u0012\u00020,0K2\b\b\u0001\u0010\u0003\u001a\u00020\u00022\b\b\u0001\u0010I\u001a\u00020H2\b\b\u0001\u0010J\u001a\u00020HH§@¢\u0006\u0005\b¤\u0001\u0010MJ$\u0010¦\u0001\u001a\b\u0012\u0004\u0012\u00020\u000f0c2\t\b\u0001\u0010B\u001a\u00030¥\u0001H§@¢\u0006\u0006\b¦\u0001\u0010§\u0001J\u0019\u0010©\u0001\u001a\t\u0012\u0005\u0012\u00030¨\u00010\bH§@¢\u0006\u0005\b©\u0001\u0010\u000b¨\u0006ª\u0001À\u0006\u0003"}, d2 = {"Lcom/skiy/lane/data/remote/api/UserApi;", MediaItem.DEFAULT_MEDIA_ID, MediaItem.DEFAULT_MEDIA_ID, "playlistId", "platform", "Lcom/skiy/lane/domain/model/LanePlaylistItem;", "o", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", MediaItem.DEFAULT_MEDIA_ID, "Lcom/skiy/lane/domain/model/LaneArtistItem;", "E", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Lcom/skiy/lane/domain/model/LaneAlbumItem;", "c0", "trackId", "Lokhttp3/ResponseBody;", "J", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "username", MediaItem.DEFAULT_MEDIA_ID, "K", "Lcom/skiy/lane/domain/model/EditProfileData;", "editProfileData", MediaItem.DEFAULT_MEDIA_ID, "F", "(Lcom/skiy/lane/domain/model/EditProfileData;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "g", "tracks", "M", "(Ljava/lang/String;Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "H", "Lcom/skiy/lane/domain/model/CreatePlaylist;", "createPlaylistModel", "Lcom/skiy/lane/domain/model/PlaylistCreationResponse;", "c", "(Lcom/skiy/lane/domain/model/CreatePlaylist;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Lcom/skiy/lane/domain/model/EditPlaylist;", "editPlaylist", "d0", "(Lcom/skiy/lane/domain/model/EditPlaylist;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "h0", "Lcom/skiy/lane/domain/model/TrackIds;", "songIds", "prefetch", "Lcom/skiy/lane/domain/model/TrackData;", CmcdData.STREAMING_FORMAT_HLS, "(Lcom/skiy/lane/domain/model/TrackIds;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "V", "deviceLanguage", "Lcom/skiy/lane/domain/model/User;", "C", "laneId", "Lcom/skiy/lane/domain/model/UserInfo;", "I", "idToken", "y", "type", "Lokhttp3/MultipartBody$Part;", UtilsKt.SCHEME_FILE, "Lcom/skiy/lane/domain/model/ImageUploadResponse;", "k", "(Ljava/lang/String;Lokhttp3/MultipartBody$Part;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "artistId", CmcdData.STREAMING_FORMAT_SS, "g0", "Lcom/skiy/lane/domain/model/SetStatusTrackRequest;", "request", "x", "(Lcom/skiy/lane/domain/model/SetStatusTrackRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "userId", "Y", "u", MediaItem.DEFAULT_MEDIA_ID, "page", "pageSize", "Lcom/skiy/lane/domain/model/PaginatedResult;", "t", "(Ljava/lang/String;IILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "R", "spotifyToken", "spotifyClientToken", "spotifyPlaylistId", "soundcloudPlaylistId", "yandexPlaylistId", "soundcloudProfileUrl", "P", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Lcom/skiy/lane/domain/model/TelegramImportCode;", "p", "r", "albumId", "f", CmcdData.OBJECT_TYPE_AUDIO_ONLY, "connectionQuality", "isVpn", "Lcom/skiy/lane/domain/model/ServerEvent;", "D", "(Ljava/lang/String;ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Lcom/skiy/lane/domain/model/UpdateFcmRequest;", "Lretrofit2/Response;", "U", "(Lcom/skiy/lane/domain/model/UpdateFcmRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Lcom/skiy/lane/domain/model/SetPlaylistVisibilityRequest;", "n", "(Ljava/lang/String;Lcom/skiy/lane/domain/model/SetPlaylistVisibilityRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Lcom/skiy/lane/domain/model/EquipBadgeRequest;", "z", "(Lcom/skiy/lane/domain/model/EquipBadgeRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Lcom/skiy/lane/domain/model/BadgeDefinition;", CmcdData.OBJECT_TYPE_MANIFEST, "filter", "Lcom/skiy/lane/domain/model/AppNotificationResponse;", "Z", "(IILjava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "notificationId", "S", "w", "Lcom/skiy/lane/domain/model/UnreadCountResponse;", "W", "Lcom/skiy/lane/domain/model/InviteUsersRequest;", "Lcom/skiy/lane/domain/model/BulkInvitationResponse;", "A", "(Ljava/lang/String;Lcom/skiy/lane/domain/model/InviteUsersRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Lcom/skiy/lane/domain/model/RespondToInvitationRequest;", "q", "(Lcom/skiy/lane/domain/model/RespondToInvitationRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "query", "X", "v", "targetUserId", "j", "Lcom/skiy/lane/domain/model/UserHomeFeed;", "T", "Lcom/skiy/lane/domain/model/UpdatePrivacyRequest;", CmcdData.OBJECT_TYPE_INIT_SEGMENT, "(Lcom/skiy/lane/domain/model/UpdatePrivacyRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Lcom/skiy/lane/domain/model/RecommendationSection;", "i0", "e0", "Lcom/skiy/lane/domain/model/UserPricing;", "B", "Lcom/skiy/lane/domain/model/ResolveArtistsRequest;", "Lcom/skiy/lane/domain/model/ResolveArtistResponse;", "G", "(Lcom/skiy/lane/domain/model/ResolveArtistsRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Lcom/skiy/lane/domain/model/CancelSubscriptionResponse;", "b", "Lcom/skiy/lane/domain/model/P2PCallbackRequest;", "d", "(Lcom/skiy/lane/domain/model/P2PCallbackRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Lcom/skiy/lane/domain/model/OnboardingArtistDto;", "O", "Lcom/skiy/lane/domain/model/SubmitOnboardingRequest;", "N", "(Lcom/skiy/lane/domain/model/SubmitOnboardingRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Lcom/skiy/lane/domain/model/CachedPlaylistStreamsResponse;", "Q", "Lcom/skiy/lane/domain/model/ReorderPlaylistRequest;", "L", "(Lcom/skiy/lane/domain/model/ReorderPlaylistRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Lcom/skiy/lane/domain/model/LaneHistoryItem;", "a0", "itemKey", "e", CmcdData.STREAM_TYPE_LIVE, "Lcom/skiy/lane/domain/model/UpdatePresenceRequest;", "b0", "(Lcom/skiy/lane/domain/model/UpdatePresenceRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Lcom/skiy/lane/domain/model/FriendPresenceDto;", "f0", "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
public interface UserApi {

    /* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
    @Metadata(k = 3, mv = {2, 2, 0}, xi = 48)
    public static final class DefaultImpls {
    }

    @POST("/playlist/{playlistId}/invite")
    Object A(@Path("playlistId") String str, @Body InviteUsersRequest inviteUsersRequest, Continuation<? super BulkInvitationResponse> continuation);

    @GET("/payment/pricing")
    Object B(Continuation<? super UserPricing> continuation);

    @GET("/account")
    Object C(@Header("DL") String str, Continuation<? super User> continuation);

    @GET("events")
    Object D(@Header("X-Connection-Quality") String str, @Header("X-Is-Vpn") boolean z, Continuation<? super List<? extends ServerEvent>> continuation);

    @GET("/user/artists")
    Object E(Continuation<? super List<LaneArtistItem>> continuation);

    @POST("/user/edit")
    Object F(@Body EditProfileData editProfileData, Continuation<? super Unit> continuation);

    @POST("/artists/resolve")
    Object G(@Body ResolveArtistsRequest resolveArtistsRequest, Continuation<? super List<ResolveArtistResponse>> continuation);

    @GET("/user/playlist/remove-track")
    Object H(@Query("playlistId") String str, @Query("trackId") String str2, Continuation<? super ResponseBody> continuation);

    @GET("/user-info")
    Object I(@Query("laneId") String str, Continuation<? super UserInfo> continuation);

    @GET("/user/history-delete-item")
    Object J(@Query("item") String str, Continuation<? super ResponseBody> continuation);

    @GET("/user/check-user-name")
    Object K(@Query("username") String str, Continuation<? super Boolean> continuation);

    @POST("/playlist/reorder")
    Object L(@Body ReorderPlaylistRequest reorderPlaylistRequest, Continuation<? super Response<Unit>> continuation);

    @POST("/user/playlist/add-tracks")
    Object M(@Query("playlistId") String str, @Body List<String> list, Continuation<? super Unit> continuation);

    @POST("/onboarding/finish")
    Object N(@Body SubmitOnboardingRequest submitOnboardingRequest, Continuation<? super Response<ResponseBody>> continuation);

    @GET("/onboarding/artists")
    Object O(@Query("page") int i, @Query("pageSize") int i2, @Query("q") String str, Continuation<? super PaginatedResult<OnboardingArtistDto>> continuation);

    @GET("user/import/preview")
    Object P(@Query("platform") String str, @Query("spotifyBearerToken") String str2, @Query("spotifyClientToken") String str3, @Query("spotifyPlaylistId") String str4, @Query("soundcloudPlaylistId") String str5, @Query("yandexPlaylistId") String str6, @Query("soundcloudProfileUrl") String str7, Continuation<? super LanePlaylistItem> continuation);

    @GET("/playlist/{playlistId}/cached-streams")
    Object Q(@Path("playlistId") String str, Continuation<? super CachedPlaylistStreamsResponse> continuation);

    @GET("user/followers")
    Object R(@Query("laneId") String str, @Query("page") int i, @Query("pageSize") int i2, Continuation<? super PaginatedResult<UserInfo>> continuation);

    @POST("notifications/{id}/read")
    Object S(@Path("id") String str, Continuation<? super Unit> continuation);

    @GET("/feed/home")
    Object T(Continuation<? super UserHomeFeed> continuation);

    @POST("user/fcm-token")
    Object U(@Body UpdateFcmRequest updateFcmRequest, Continuation<? super Response<ResponseBody>> continuation);

    @GET("/user/playlists")
    Object V(Continuation<? super List<LanePlaylistItem>> continuation);

    @GET("notifications/unread-count")
    Object W(Continuation<? super UnreadCountResponse> continuation);

    @GET("/user/friends")
    Object X(@Query("page") int i, @Query("pageSize") int i2, @Query("q") String str, Continuation<? super PaginatedResult<UserInfo>> continuation);

    @POST("user/follow/{id}")
    Object Y(@Path("id") String str, Continuation<? super Unit> continuation);

    @GET("notifications")
    Object Z(@Query("page") int i, @Query("pageSize") int i2, @Query("filter") String str, Continuation<? super PaginatedResult<AppNotificationResponse>> continuation);

    @DELETE("user/unsubscribe/album/{id}")
    Object a(@Path("id") String str, Continuation<? super Unit> continuation);

    @GET("/user/search/history")
    Object a0(Continuation<? super List<? extends LaneHistoryItem>> continuation);

    @POST("/payment/cancel-subscription")
    Object b(Continuation<? super CancelSubscriptionResponse> continuation);

    @POST("/user/presence")
    Object b0(@Body UpdatePresenceRequest updatePresenceRequest, Continuation<? super Response<ResponseBody>> continuation);

    @POST("/create-playlist")
    Object c(@Body CreatePlaylist createPlaylist, Continuation<? super PlaylistCreationResponse> continuation);

    @GET("/user/albums")
    Object c0(Continuation<? super List<LaneAlbumItem>> continuation);

    @POST("/p2p/callback")
    Object d(@Body P2PCallbackRequest p2PCallbackRequest, Continuation<? super Unit> continuation);

    @POST("/edit-playlist")
    Object d0(@Body EditPlaylist editPlaylist, Continuation<? super ResponseBody> continuation);

    @GET("/user/history-bump-item")
    Object e(@Query("item") String str, Continuation<? super ResponseBody> continuation);

    @GET("/users/search")
    Object e0(@Query("q") String str, Continuation<? super List<UserInfo>> continuation);

    @POST("user/subscribe/album/{id}")
    Object f(@Path("id") String str, Continuation<? super Unit> continuation);

    @GET("/user/friends/presence")
    Object f0(Continuation<? super List<FriendPresenceDto>> continuation);

    @GET("/user/playlist/add")
    Object g(@Query("playlistId") String str, Continuation<? super ResponseBody> continuation);

    @DELETE("/user/unsubscribe/artist/{id}")
    Object g0(@Path("id") String str, Continuation<? super ResponseBody> continuation);

    @POST("/user/tracks")
    Object h(@Body TrackIds trackIds, @Query("prefetch") String str, Continuation<? super List<TrackData>> continuation);

    @GET("/delete-playlist")
    Object h0(@Query("playlistId") String str, Continuation<? super ResponseBody> continuation);

    @POST("/user/settings/privacy")
    Object i(@Body UpdatePrivacyRequest updatePrivacyRequest, Continuation<? super Response<ResponseBody>> continuation);

    @GET("/user/recent")
    Object i0(Continuation<? super RecommendationSection> continuation);

    @DELETE("/playlist/{playlistId}/collaborators/{targetUserId}")
    Object j(@Path("playlistId") String str, @Path("targetUserId") String str2, Continuation<? super Response<ResponseBody>> continuation);

    @POST("/user/upload/photo")
    @Multipart
    Object k(@Query("type") String str, @Part MultipartBody.Part part, Continuation<? super ImageUploadResponse> continuation);

    @GET("/playlist/{playlistId}/tracks")
    Object l(@Path("playlistId") String str, @Query("page") int i, @Query("pageSize") int i2, Continuation<? super PaginatedResult<TrackData>> continuation);

    @GET("badges/definitions")
    Object m(Continuation<? super List<BadgeDefinition>> continuation);

    @POST("playlist/{playlistId}/visibility")
    Object n(@Path("playlistId") String str, @Body SetPlaylistVisibilityRequest setPlaylistVisibilityRequest, Continuation<? super Unit> continuation);

    @GET("/playlist/{playlistId}")
    Object o(@Path("playlistId") String str, @Query("platform") String str2, Continuation<? super LanePlaylistItem> continuation);

    @GET("import/telegram/start")
    Object p(Continuation<? super TelegramImportCode> continuation);

    @POST("/playlist/invite/respond")
    Object q(@Body RespondToInvitationRequest respondToInvitationRequest, Continuation<? super Response<ResponseBody>> continuation);

    @GET("import/telegram/finish")
    Object r(Continuation<? super LanePlaylistItem> continuation);

    @POST("/user/subscribe/artist/{id}")
    Object s(@Path("id") String str, Continuation<? super ResponseBody> continuation);

    @GET("user/following")
    Object t(@Query("laneId") String str, @Query("page") int i, @Query("pageSize") int i2, Continuation<? super PaginatedResult<UserInfo>> continuation);

    @DELETE("user/unfollow/{id}")
    Object u(@Path("id") String str, Continuation<? super Unit> continuation);

    @GET("/playlist/{playlistId}/collaborators")
    Object v(@Path("playlistId") String str, Continuation<? super List<UserInfo>> continuation);

    @POST("notifications/read-all")
    Object w(Continuation<? super Unit> continuation);

    @POST("user/status")
    Object x(@Body SetStatusTrackRequest setStatusTrackRequest, Continuation<? super Unit> continuation);

    @GET("/createUserOrLogin")
    Object y(@Query("idToken") String str, Continuation<? super ResponseBody> continuation);

    @POST("user/badge/equip")
    Object z(@Body EquipBadgeRequest equipBadgeRequest, Continuation<? super Unit> continuation);
}
