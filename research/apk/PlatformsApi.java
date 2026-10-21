package com.skiy.lane.data.remote.api;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.upstream.CmcdData;
import com.skiy.lane.domain.model.ArtistCandle;
import com.skiy.lane.domain.model.LaneAlbumItem;
import com.skiy.lane.domain.model.LaneArtistItem;
import com.skiy.lane.domain.model.PaginatedResult;
import com.skiy.lane.domain.model.PlaceCandleRequest;
import com.skiy.lane.domain.model.SearchClickEvent;
import com.skiy.lane.domain.model.SmartHintsResponse;
import kotlin.Metadata;
import kotlin.coroutines.Continuation;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes.dex */
@Metadata(d1 = {"\u0000V\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\bf\u0018\u00002\u00020\u0001J\u001a\u0010\u0005\u001a\u00020\u00042\b\b\u0001\u0010\u0003\u001a\u00020\u0002H§@¢\u0006\u0004\b\u0005\u0010\u0006J\u001a\u0010\t\u001a\u00020\b2\b\b\u0001\u0010\u0007\u001a\u00020\u0002H§@¢\u0006\u0004\b\t\u0010\u0006J\u001a\u0010\f\u001a\u00020\u000b2\b\b\u0001\u0010\n\u001a\u00020\u0002H§@¢\u0006\u0004\b\f\u0010\u0006J\u001a\u0010\u0010\u001a\u00020\u000f2\b\b\u0001\u0010\u000e\u001a\u00020\rH§@¢\u0006\u0004\b\u0010\u0010\u0011J\u001a\u0010\u0013\u001a\u00020\u00122\b\b\u0001\u0010\u0003\u001a\u00020\u0002H§@¢\u0006\u0004\b\u0013\u0010\u0006J$\u0010\u0017\u001a\u00020\u00162\b\b\u0001\u0010\u0003\u001a\u00020\u00022\b\b\u0001\u0010\u0015\u001a\u00020\u0014H§@¢\u0006\u0004\b\u0017\u0010\u0018J4\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00160\u001c2\b\b\u0001\u0010\u0003\u001a\u00020\u00022\b\b\u0001\u0010\u001a\u001a\u00020\u00192\b\b\u0001\u0010\u001b\u001a\u00020\u0019H§@¢\u0006\u0004\b\u001d\u0010\u001e¨\u0006\u001fÀ\u0006\u0003"}, d2 = {"Lcom/skiy/lane/data/remote/api/PlatformsApi;", MediaItem.DEFAULT_MEDIA_ID, MediaItem.DEFAULT_MEDIA_ID, "artistId", "Lcom/skiy/lane/domain/model/LaneArtistItem;", "b", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "albumId", "Lcom/skiy/lane/domain/model/LaneAlbumItem;", "g", "query", "Lcom/skiy/lane/domain/model/SmartHintsResponse;", "e", "Lcom/skiy/lane/domain/model/SearchClickEvent;", "clickEvent", "Lokhttp3/ResponseBody;", "d", "(Lcom/skiy/lane/domain/model/SearchClickEvent;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", MediaItem.DEFAULT_MEDIA_ID, CmcdData.OBJECT_TYPE_AUDIO_ONLY, "Lcom/skiy/lane/domain/model/PlaceCandleRequest;", "request", "Lcom/skiy/lane/domain/model/ArtistCandle;", "c", "(Ljava/lang/String;Lcom/skiy/lane/domain/model/PlaceCandleRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", MediaItem.DEFAULT_MEDIA_ID, "page", "pageSize", "Lcom/skiy/lane/domain/model/PaginatedResult;", "f", "(Ljava/lang/String;IILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
public interface PlatformsApi {
    @GET("/platforms/artist/rip/candles-count")
    Object a(@Query("artistId") String str, Continuation<? super Long> continuation);

    @GET("/platforms/artist")
    Object b(@Query("artistId") String str, Continuation<? super LaneArtistItem> continuation);

    @POST("artist/{artistId}/candle")
    Object c(@Path("artistId") String str, @Body PlaceCandleRequest placeCandleRequest, Continuation<? super ArtistCandle> continuation);

    @POST("/platforms/search/click")
    Object d(@Body SearchClickEvent searchClickEvent, Continuation<? super ResponseBody> continuation);

    @GET("/platforms/v2/hints")
    Object e(@Query("q") String str, Continuation<? super SmartHintsResponse> continuation);

    @GET("artist/{artistId}/candles")
    Object f(@Path("artistId") String str, @Query("page") int i, @Query("pageSize") int i2, Continuation<? super PaginatedResult<ArtistCandle>> continuation);

    @GET("/platforms/album")
    Object g(@Query("albumId") String str, Continuation<? super LaneAlbumItem> continuation);
}
