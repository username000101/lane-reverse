package com.skiy.lane.domain.model;

import androidx.media3.common.MediaItem;
import defpackage.b60;
import kotlin.Metadata;
import kotlin.jvm.JvmStatic;
import kotlin.jvm.internal.Intrinsics;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.Serializable;
import kotlinx.serialization.descriptors.SerialDescriptor;
import kotlinx.serialization.encoding.CompositeEncoder;
import kotlinx.serialization.internal.PluginExceptionsKt;
import kotlinx.serialization.internal.SerializationConstructorMarker;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes3.dex */
@Metadata(d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0087\b\u0018\u0000 %2\u00020\u0001:\u0002$%B!\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0007¢\u0006\u0004\b\b\u0010\tB7\b\u0010\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\b\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u0012\b\u0010\f\u001a\u0004\u0018\u00010\r¢\u0006\u0004\b\b\u0010\u000eJ\t\u0010\u0014\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0015\u001a\u00020\u0005HÆ\u0003J\u000b\u0010\u0016\u001a\u0004\u0018\u00010\u0007HÆ\u0003J)\u0010\u0017\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u0007HÆ\u0001J\u0013\u0010\u0018\u001a\u00020\u00052\b\u0010\u0019\u001a\u0004\u0018\u00010\u0001HÖ\u0003J\t\u0010\u001a\u001a\u00020\u000bHÖ\u0001J\t\u0010\u001b\u001a\u00020\u0003HÖ\u0001J%\u0010\u001c\u001a\u00020\u001d2\u0006\u0010\u001e\u001a\u00020\u00002\u0006\u0010\u001f\u001a\u00020 2\u0006\u0010!\u001a\u00020\"H\u0001¢\u0006\u0002\b#R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0011\u0010\u0004\u001a\u00020\u0005¢\u0006\b\n\u0000\u001a\u0004\b\u0004\u0010\u0011R\u0013\u0010\u0006\u001a\u0004\u0018\u00010\u0007¢\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013¨\u0006&"}, d2 = {"Lcom/skiy/lane/domain/model/LaneTokenResponse;", MediaItem.DEFAULT_MEDIA_ID, "token", MediaItem.DEFAULT_MEDIA_ID, "isFirstAuth", MediaItem.DEFAULT_MEDIA_ID, "user", "Lcom/skiy/lane/domain/model/User;", "<init>", "(Ljava/lang/String;ZLcom/skiy/lane/domain/model/User;)V", "seen0", MediaItem.DEFAULT_MEDIA_ID, "serializationConstructorMarker", "Lkotlinx/serialization/internal/SerializationConstructorMarker;", "(ILjava/lang/String;ZLcom/skiy/lane/domain/model/User;Lkotlinx/serialization/internal/SerializationConstructorMarker;)V", "getToken", "()Ljava/lang/String;", "()Z", "getUser", "()Lcom/skiy/lane/domain/model/User;", "component1", "component2", "component3", "copy", "equals", "other", "hashCode", "toString", "write$Self", MediaItem.DEFAULT_MEDIA_ID, "self", "output", "Lkotlinx/serialization/encoding/CompositeEncoder;", "serialDesc", "Lkotlinx/serialization/descriptors/SerialDescriptor;", "write$Self$app_release", "$serializer", "Companion", "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
@Serializable
public final /* data */ class LaneTokenResponse {
    public static final int $stable = 8;

    /* JADX INFO: renamed from: Companion, reason: from kotlin metadata */
    public static final Companion INSTANCE = new Companion();
    private final boolean isFirstAuth;
    private final String token;
    private final User user;

    /* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
    @Metadata(d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001J\u0013\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00030\u0002¢\u0006\u0004\b\u0004\u0010\u0005¨\u0006\u0006"}, d2 = {"Lcom/skiy/lane/domain/model/LaneTokenResponse$Companion;", MediaItem.DEFAULT_MEDIA_ID, "Lkotlinx/serialization/KSerializer;", "Lcom/skiy/lane/domain/model/LaneTokenResponse;", "serializer", "()Lkotlinx/serialization/KSerializer;", "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
    public static final class Companion {
        public final KSerializer<LaneTokenResponse> serializer() {
            return LaneTokenResponse$$serializer.INSTANCE;
        }
    }

    public /* synthetic */ LaneTokenResponse(int i, String str, boolean z, User user, SerializationConstructorMarker serializationConstructorMarker) {
        if (7 != (i & 7)) {
            PluginExceptionsKt.a(i, 7, LaneTokenResponse$$serializer.INSTANCE.getDescriptor());
            throw null;
        }
        this.token = str;
        this.isFirstAuth = z;
        this.user = user;
    }

    public static /* synthetic */ LaneTokenResponse copy$default(LaneTokenResponse laneTokenResponse, String str, boolean z, User user, int i, Object obj) {
        if ((i & 1) != 0) {
            str = laneTokenResponse.token;
        }
        if ((i & 2) != 0) {
            z = laneTokenResponse.isFirstAuth;
        }
        if ((i & 4) != 0) {
            user = laneTokenResponse.user;
        }
        return laneTokenResponse.copy(str, z, user);
    }

    @JvmStatic
    public static final /* synthetic */ void write$Self$app_release(LaneTokenResponse self, CompositeEncoder output, SerialDescriptor serialDesc) {
        output.A(serialDesc, 0, self.token);
        output.z(serialDesc, 1, self.isFirstAuth);
        output.i(serialDesc, 2, User$$serializer.INSTANCE, self.user);
    }

    /* JADX INFO: renamed from: component1, reason: from getter */
    public final String getToken() {
        return this.token;
    }

    /* JADX INFO: renamed from: component2, reason: from getter */
    public final boolean getIsFirstAuth() {
        return this.isFirstAuth;
    }

    /* JADX INFO: renamed from: component3, reason: from getter */
    public final User getUser() {
        return this.user;
    }

    public final LaneTokenResponse copy(String token, boolean isFirstAuth, User user) {
        token.getClass();
        return new LaneTokenResponse(token, isFirstAuth, user);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LaneTokenResponse)) {
            return false;
        }
        LaneTokenResponse laneTokenResponse = (LaneTokenResponse) other;
        return Intrinsics.c(this.token, laneTokenResponse.token) && this.isFirstAuth == laneTokenResponse.isFirstAuth && Intrinsics.c(this.user, laneTokenResponse.user);
    }

    public final String getToken() {
        return this.token;
    }

    public final User getUser() {
        return this.user;
    }

    public int hashCode() {
        int iD = b60.d(this.token.hashCode() * 31, 31, this.isFirstAuth);
        User user = this.user;
        return iD + (user == null ? 0 : user.hashCode());
    }

    public final boolean isFirstAuth() {
        return this.isFirstAuth;
    }

    public String toString() {
        return "LaneTokenResponse(token=" + this.token + ", isFirstAuth=" + this.isFirstAuth + ", user=" + this.user + ")";
    }

    public LaneTokenResponse(String str, boolean z, User user) {
        str.getClass();
        this.token = str;
        this.isFirstAuth = z;
        this.user = user;
    }
}
