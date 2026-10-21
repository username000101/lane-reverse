package com.skiy.lane.data.remote.api;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.upstream.CmcdData;
import com.skiy.lane.domain.model.LaneCombinedSearchResponse;
import com.skiy.lane.domain.model.LanePlaylistItem;
import com.skiy.lane.domain.model.LaneShareItem;
import com.skiy.lane.domain.model.TrackLyrics;
import com.skiy.lane.domain.model.TrackStats;
import com.skiy.lane.domain.model.TrackStreamingResult;
import kotlin.Metadata;
import kotlin.coroutines.Continuation;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes.dex */
@Metadata(d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J.\u0010\u0007\u001a\u00020\u00062\b\b\u0001\u0010\u0003\u001a\u00020\u00022\b\b\u0001\u0010\u0004\u001a\u00020\u00022\b\b\u0001\u0010\u0005\u001a\u00020\u0002H§@¢\u0006\u0004\b\u0007\u0010\bJ$\u0010\u000b\u001a\u00020\n2\b\b\u0001\u0010\t\u001a\u00020\u00022\b\b\u0001\u0010\u0004\u001a\u00020\u0002H§@¢\u0006\u0004\b\u000b\u0010\fJ2\u0010\u0010\u001a\u00020\u000f2\b\b\u0001\u0010\t\u001a\u00020\u00022\n\b\u0001\u0010\r\u001a\u0004\u0018\u00010\u00022\n\b\u0001\u0010\u000e\u001a\u0004\u0018\u00010\u0002H§@¢\u0006\u0004\b\u0010\u0010\bJ\u001a\u0010\u0012\u001a\u00020\u00112\b\b\u0001\u0010\t\u001a\u00020\u0002H§@¢\u0006\u0004\b\u0012\u0010\u0013J$\u0010\u0015\u001a\u00020\u000f2\b\b\u0001\u0010\u0014\u001a\u00020\u00022\b\b\u0001\u0010\t\u001a\u00020\u0002H§@¢\u0006\u0004\b\u0015\u0010\fJ&\u0010\u0016\u001a\u00020\u000f2\b\b\u0001\u0010\t\u001a\u00020\u00022\n\b\u0001\u0010\u000e\u001a\u0004\u0018\u00010\u0002H§@¢\u0006\u0004\b\u0016\u0010\fJ$\u0010\u0019\u001a\u00020\u00182\b\b\u0001\u0010\t\u001a\u00020\u00022\b\b\u0001\u0010\u0017\u001a\u00020\u0002H§@¢\u0006\u0004\b\u0019\u0010\fJ\u001a\u0010\u001b\u001a\u00020\u001a2\b\b\u0001\u0010\t\u001a\u00020\u0002H§@¢\u0006\u0004\b\u001b\u0010\u0013J\u001a\u0010\u001e\u001a\u00020\u001d2\b\b\u0001\u0010\u001c\u001a\u00020\u0002H§@¢\u0006\u0004\b\u001e\u0010\u0013¨\u0006\u001fÀ\u0006\u0003"}, d2 = {"Lcom/skiy/lane/data/remote/api/TracksApi;", MediaItem.DEFAULT_MEDIA_ID, MediaItem.DEFAULT_MEDIA_ID, "query", "platform", "version", "Lcom/skiy/lane/domain/model/LaneCombinedSearchResponse;", "d", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "trackId", "Lcom/skiy/lane/domain/model/LanePlaylistItem;", "c", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "refId", "streamQuality", "Lcom/skiy/lane/domain/model/TrackStreamingResult;", CmcdData.OBJECT_TYPE_INIT_SEGMENT, "Lcom/skiy/lane/domain/model/TrackStats;", CmcdData.OBJECT_TYPE_AUDIO_ONLY, "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "effect", "g", "e", "shareType", "Lcom/skiy/lane/domain/model/LaneShareItem;", "f", "Lcom/skiy/lane/domain/model/TrackLyrics;", CmcdData.STREAMING_FORMAT_HLS, "token", "Lokhttp3/ResponseBody;", "b", "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
public interface TracksApi {
    @GET("/track/stats")
    Object a(@Query("trackId") String str, Continuation<? super TrackStats> continuation);

    @POST("/track/log")
    Object b(@Query("token") String str, Continuation<? super ResponseBody> continuation);

    @GET("/platforms/recommendations")
    Object c(@Query("trackId") String str, @Query("platform") String str2, Continuation<? super LanePlaylistItem> continuation);

    @GET("/platforms/search")
    Object d(@Query("q") String str, @Query("platform") String str2, @Query("ver") String str3, Continuation<? super LaneCombinedSearchResponse> continuation);

    @Streaming
    @GET("/track/download")
    Object e(@Query("trackId") String str, @Query("streamQuality") String str2, Continuation<? super TrackStreamingResult> continuation);

    @GET("/share/create")
    Object f(@Query("shareElementId") String str, @Query("shareType") String str2, Continuation<? super LaneShareItem> continuation);

    @GET("/track/effect")
    Object g(@Query("effect") String str, @Query("trackId") String str2, Continuation<? super TrackStreamingResult> continuation);

    @GET("track/{trackId}/lyrics")
    Object h(@Path("trackId") String str, Continuation<? super TrackLyrics> continuation);

    @GET("/track/stream")
    Object i(@Query("trackId") String str, @Query("refId") String str2, @Query("streamQuality") String str3, Continuation<? super TrackStreamingResult> continuation);
}
