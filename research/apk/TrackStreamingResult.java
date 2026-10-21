package com.skiy.lane.domain.model;

import androidx.media3.common.MediaItem;
import defpackage.b60;
import kotlin.Metadata;
import kotlin.jvm.JvmStatic;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.Serializable;
import kotlinx.serialization.descriptors.SerialDescriptor;
import kotlinx.serialization.encoding.CompositeEncoder;
import kotlinx.serialization.internal.PluginExceptionsKt;
import kotlinx.serialization.internal.SerializationConstructorMarker;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes3.dex */
@Metadata(d1 = {"\u0000F\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\r\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0087\b\u0018\u0000 '2\u00020\u0001:\u0002'(B)\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0003\u0012\u0006\u0010\u0006\u001a\u00020\u0007¢\u0006\u0004\b\b\u0010\tBA\b\u0010\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\b\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\b\u0010\f\u001a\u0004\u0018\u00010\r¢\u0006\u0004\b\b\u0010\u000eJ\t\u0010\u0015\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0016\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0017\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0018\u001a\u00020\u0007HÆ\u0003J1\u0010\u0019\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u0007HÆ\u0001J\u0013\u0010\u001a\u001a\u00020\u001b2\b\u0010\u001c\u001a\u0004\u0018\u00010\u0001HÖ\u0003J\t\u0010\u001d\u001a\u00020\u000bHÖ\u0001J\t\u0010\u001e\u001a\u00020\u0003HÖ\u0001J%\u0010\u001f\u001a\u00020 2\u0006\u0010!\u001a\u00020\u00002\u0006\u0010\"\u001a\u00020#2\u0006\u0010$\u001a\u00020%H\u0001¢\u0006\u0002\b&R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0011\u0010\u0004\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0010R\u0011\u0010\u0005\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0010R\u0011\u0010\u0006\u001a\u00020\u0007¢\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014¨\u0006)"}, d2 = {"Lcom/skiy/lane/domain/model/TrackStreamingResult;", MediaItem.DEFAULT_MEDIA_ID, "url", MediaItem.DEFAULT_MEDIA_ID, "trackId", "playbackToken", "ttl", MediaItem.DEFAULT_MEDIA_ID, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;J)V", "seen0", MediaItem.DEFAULT_MEDIA_ID, "serializationConstructorMarker", "Lkotlinx/serialization/internal/SerializationConstructorMarker;", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;JLkotlinx/serialization/internal/SerializationConstructorMarker;)V", "getUrl", "()Ljava/lang/String;", "getTrackId", "getPlaybackToken", "getTtl", "()J", "component1", "component2", "component3", "component4", "copy", "equals", MediaItem.DEFAULT_MEDIA_ID, "other", "hashCode", "toString", "write$Self", MediaItem.DEFAULT_MEDIA_ID, "self", "output", "Lkotlinx/serialization/encoding/CompositeEncoder;", "serialDesc", "Lkotlinx/serialization/descriptors/SerialDescriptor;", "write$Self$app_release", "Companion", "$serializer", "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
@Serializable
public final /* data */ class TrackStreamingResult {
    public static final int $stable = 0;

    /* JADX INFO: renamed from: Companion, reason: from kotlin metadata */
    public static final Companion INSTANCE = new Companion();
    private static final TrackStreamingResult EMPTY = new TrackStreamingResult(MediaItem.DEFAULT_MEDIA_ID, MediaItem.DEFAULT_MEDIA_ID, MediaItem.DEFAULT_MEDIA_ID, 90000);
    private final String playbackToken;
    private final String trackId;
    private final long ttl;
    private final String url;

    /* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
    @Metadata(d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001J\u0013\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00030\u0002¢\u0006\u0004\b\u0004\u0010\u0005¨\u0006\u0006"}, d2 = {"Lcom/skiy/lane/domain/model/TrackStreamingResult$Companion;", MediaItem.DEFAULT_MEDIA_ID, "Lkotlinx/serialization/KSerializer;", "Lcom/skiy/lane/domain/model/TrackStreamingResult;", "serializer", "()Lkotlinx/serialization/KSerializer;", "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
    public static final class Companion {
        public final KSerializer<TrackStreamingResult> serializer() {
            return TrackStreamingResult$$serializer.INSTANCE;
        }
    }

    public /* synthetic */ TrackStreamingResult(int i, String str, String str2, String str3, long j, SerializationConstructorMarker serializationConstructorMarker) {
        if (11 != (i & 11)) {
            PluginExceptionsKt.a(i, 11, TrackStreamingResult$$serializer.INSTANCE.getDescriptor());
            throw null;
        }
        this.url = str;
        this.trackId = str2;
        if ((i & 4) == 0) {
            this.playbackToken = MediaItem.DEFAULT_MEDIA_ID;
        } else {
            this.playbackToken = str3;
        }
        this.ttl = j;
    }

    public static /* synthetic */ TrackStreamingResult copy$default(TrackStreamingResult trackStreamingResult, String str, String str2, String str3, long j, int i, Object obj) {
        if ((i & 1) != 0) {
            str = trackStreamingResult.url;
        }
        if ((i & 2) != 0) {
            str2 = trackStreamingResult.trackId;
        }
        if ((i & 4) != 0) {
            str3 = trackStreamingResult.playbackToken;
        }
        if ((i & 8) != 0) {
            j = trackStreamingResult.ttl;
        }
        String str4 = str3;
        return trackStreamingResult.copy(str, str2, str4, j);
    }

    @JvmStatic
    public static final /* synthetic */ void write$Self$app_release(TrackStreamingResult self, CompositeEncoder output, SerialDescriptor serialDesc) {
        output.A(serialDesc, 0, self.url);
        output.A(serialDesc, 1, self.trackId);
        if (output.q(serialDesc) || !Intrinsics.c(self.playbackToken, MediaItem.DEFAULT_MEDIA_ID)) {
            output.A(serialDesc, 2, self.playbackToken);
        }
        output.E(serialDesc, 3, self.ttl);
    }

    /* JADX INFO: renamed from: component1, reason: from getter */
    public final String getUrl() {
        return this.url;
    }

    /* JADX INFO: renamed from: component2, reason: from getter */
    public final String getTrackId() {
        return this.trackId;
    }

    /* JADX INFO: renamed from: component3, reason: from getter */
    public final String getPlaybackToken() {
        return this.playbackToken;
    }

    /* JADX INFO: renamed from: component4, reason: from getter */
    public final long getTtl() {
        return this.ttl;
    }

    public final TrackStreamingResult copy(String url, String trackId, String playbackToken, long ttl) {
        url.getClass();
        trackId.getClass();
        playbackToken.getClass();
        return new TrackStreamingResult(url, trackId, playbackToken, ttl);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TrackStreamingResult)) {
            return false;
        }
        TrackStreamingResult trackStreamingResult = (TrackStreamingResult) other;
        return Intrinsics.c(this.url, trackStreamingResult.url) && Intrinsics.c(this.trackId, trackStreamingResult.trackId) && Intrinsics.c(this.playbackToken, trackStreamingResult.playbackToken) && this.ttl == trackStreamingResult.ttl;
    }

    public final String getPlaybackToken() {
        return this.playbackToken;
    }

    public final String getTrackId() {
        return this.trackId;
    }

    public final long getTtl() {
        return this.ttl;
    }

    public final String getUrl() {
        return this.url;
    }

    public int hashCode() {
        return Long.hashCode(this.ttl) + b60.c(b60.c(this.url.hashCode() * 31, 31, this.trackId), 31, this.playbackToken);
    }

    public String toString() {
        String str = this.url;
        String str2 = this.trackId;
        String str3 = this.playbackToken;
        long j = this.ttl;
        StringBuilder sbK = b60.k("TrackStreamingResult(url=", str, ", trackId=", str2, ", playbackToken=");
        sbK.append(str3);
        sbK.append(", ttl=");
        sbK.append(j);
        sbK.append(")");
        return sbK.toString();
    }

    public TrackStreamingResult(String str, String str2, String str3, long j) {
        str.getClass();
        str2.getClass();
        str3.getClass();
        this.url = str;
        this.trackId = str2;
        this.playbackToken = str3;
        this.ttl = j;
    }

    public /* synthetic */ TrackStreamingResult(String str, String str2, String str3, long j, int i, DefaultConstructorMarker defaultConstructorMarker) {
        this(str, str2, (i & 4) != 0 ? MediaItem.DEFAULT_MEDIA_ID : str3, j);
    }
}
