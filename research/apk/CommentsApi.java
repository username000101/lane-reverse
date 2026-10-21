package com.skiy.lane.data.remote.api;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.upstream.CmcdData;
import com.skiy.lane.domain.model.CommentSortBy;
import com.skiy.lane.domain.model.CreateReply;
import com.skiy.lane.domain.model.CreateTrackComment;
import com.skiy.lane.domain.model.CreatedCommentResponse;
import com.skiy.lane.domain.model.CreatedReplyResponse;
import com.skiy.lane.domain.model.LaneTrackComment;
import com.skiy.lane.domain.model.PaginatedResult;
import kotlin.Metadata;
import kotlin.coroutines.Continuation;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes.dex */
@Metadata(d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\bf\u0018\u00002\u00020\u0001JJ\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000b0\n2\b\b\u0001\u0010\u0003\u001a\u00020\u00022\b\b\u0001\u0010\u0005\u001a\u00020\u00042\b\b\u0001\u0010\u0006\u001a\u00020\u00042\b\b\u0003\u0010\b\u001a\u00020\u00072\n\b\u0001\u0010\t\u001a\u0004\u0018\u00010\u0002H§@¢\u0006\u0004\b\f\u0010\rJ4\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u000b0\n2\b\b\u0001\u0010\u000e\u001a\u00020\u00022\b\b\u0001\u0010\u0005\u001a\u00020\u00042\b\b\u0001\u0010\u0006\u001a\u00020\u0004H§@¢\u0006\u0004\b\u000f\u0010\u0010J\u001a\u0010\u0014\u001a\u00020\u00132\b\b\u0001\u0010\u0012\u001a\u00020\u0011H§@¢\u0006\u0004\b\u0014\u0010\u0015J$\u0010\u0018\u001a\u00020\u00172\b\b\u0001\u0010\u000e\u001a\u00020\u00022\b\b\u0001\u0010\u0012\u001a\u00020\u0016H§@¢\u0006\u0004\b\u0018\u0010\u0019J\u001a\u0010\u001b\u001a\u00020\u001a2\b\b\u0001\u0010\u000e\u001a\u00020\u0002H§@¢\u0006\u0004\b\u001b\u0010\u001cJ\u001a\u0010\u001d\u001a\u00020\u001a2\b\b\u0001\u0010\u000e\u001a\u00020\u0002H§@¢\u0006\u0004\b\u001d\u0010\u001c¨\u0006\u001eÀ\u0006\u0003"}, d2 = {"Lcom/skiy/lane/data/remote/api/CommentsApi;", MediaItem.DEFAULT_MEDIA_ID, MediaItem.DEFAULT_MEDIA_ID, "trackId", MediaItem.DEFAULT_MEDIA_ID, "page", "pageSize", "Lcom/skiy/lane/domain/model/CommentSortBy;", "sortBy", "highlightCommentId", "Lcom/skiy/lane/domain/model/PaginatedResult;", "Lcom/skiy/lane/domain/model/LaneTrackComment;", "d", "(Ljava/lang/String;IILcom/skiy/lane/domain/model/CommentSortBy;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "commentId", CmcdData.OBJECT_TYPE_AUDIO_ONLY, "(Ljava/lang/String;IILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Lcom/skiy/lane/domain/model/CreateTrackComment;", "body", "Lcom/skiy/lane/domain/model/CreatedCommentResponse;", "f", "(Lcom/skiy/lane/domain/model/CreateTrackComment;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Lcom/skiy/lane/domain/model/CreateReply;", "Lcom/skiy/lane/domain/model/CreatedReplyResponse;", "c", "(Ljava/lang/String;Lcom/skiy/lane/domain/model/CreateReply;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Lokhttp3/ResponseBody;", "b", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "e", "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
public interface CommentsApi {

    /* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
    @Metadata(k = 3, mv = {2, 2, 0}, xi = 48)
    public static final class DefaultImpls {
    }

    @GET("comment/{commentId}/replies")
    Object a(@Path("commentId") String str, @Query("page") int i, @Query("pageSize") int i2, Continuation<? super PaginatedResult<LaneTrackComment>> continuation);

    @POST("comment/{commentId}/like")
    Object b(@Path("commentId") String str, Continuation<? super ResponseBody> continuation);

    @POST("comment/{commentId}/reply")
    Object c(@Path("commentId") String str, @Body CreateReply createReply, Continuation<? super CreatedReplyResponse> continuation);

    @GET("track/{trackId}/comments")
    Object d(@Path("trackId") String str, @Query("page") int i, @Query("pageSize") int i2, @Query("sortBy") CommentSortBy commentSortBy, @Query("highlightCommentId") String str2, Continuation<? super PaginatedResult<LaneTrackComment>> continuation);

    @DELETE("comment/{commentId}/like")
    Object e(@Path("commentId") String str, Continuation<? super ResponseBody> continuation);

    @POST("comment/track")
    Object f(@Body CreateTrackComment createTrackComment, Continuation<? super CreatedCommentResponse> continuation);
}
