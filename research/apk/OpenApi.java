package com.skiy.lane.data.remote.api;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.upstream.CmcdData;
import com.skiy.lane.domain.model.LaneOpenShareItem;
import kotlin.Metadata;
import kotlin.coroutines.Continuation;
import retrofit2.http.GET;
import retrofit2.http.Query;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes.dex */
@Metadata(d1 = {"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\bf\u0018\u00002\u00020\u0001J\u001a\u0010\u0005\u001a\u00020\u00042\b\b\u0001\u0010\u0003\u001a\u00020\u0002H§@¢\u0006\u0004\b\u0005\u0010\u0006¨\u0006\u0007À\u0006\u0003"}, d2 = {"Lcom/skiy/lane/data/remote/api/OpenApi;", MediaItem.DEFAULT_MEDIA_ID, MediaItem.DEFAULT_MEDIA_ID, "id", "Lcom/skiy/lane/domain/model/LaneOpenShareItem;", CmcdData.OBJECT_TYPE_AUDIO_ONLY, "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
public interface OpenApi {
    @GET("/v1/share/get")
    Object a(@Query("id") String str, Continuation<? super LaneOpenShareItem> continuation);
}
