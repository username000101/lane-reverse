package com.skiy.lane.domain.model;

import androidx.media3.common.MediaItem;
import defpackage.b60;
import defpackage.w;
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
@Metadata(d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0087\b\u0018\u0000 #2\u00020\u0001:\u0002\"#B\u001f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0003¢\u0006\u0004\b\u0006\u0010\u0007B9\b\u0010\u0012\u0006\u0010\b\u001a\u00020\t\u0012\b\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u0012\b\u0010\n\u001a\u0004\u0018\u00010\u000b¢\u0006\u0004\b\u0006\u0010\fJ\t\u0010\u0011\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0012\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0013\u001a\u00020\u0003HÆ\u0003J'\u0010\u0014\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u0003HÆ\u0001J\u0013\u0010\u0015\u001a\u00020\u00162\b\u0010\u0017\u001a\u0004\u0018\u00010\u0001HÖ\u0003J\t\u0010\u0018\u001a\u00020\tHÖ\u0001J\t\u0010\u0019\u001a\u00020\u0003HÖ\u0001J%\u0010\u001a\u001a\u00020\u001b2\u0006\u0010\u001c\u001a\u00020\u00002\u0006\u0010\u001d\u001a\u00020\u001e2\u0006\u0010\u001f\u001a\u00020 H\u0001¢\u0006\u0002\b!R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0011\u0010\u0004\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u000eR\u0011\u0010\u0005\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u000e¨\u0006$"}, d2 = {"Lcom/skiy/lane/domain/model/ApiReserveResponse;", MediaItem.DEFAULT_MEDIA_ID, "apiUrl", MediaItem.DEFAULT_MEDIA_ID, "cdnUrl", "countryCode", "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "seen0", MediaItem.DEFAULT_MEDIA_ID, "serializationConstructorMarker", "Lkotlinx/serialization/internal/SerializationConstructorMarker;", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlinx/serialization/internal/SerializationConstructorMarker;)V", "getApiUrl", "()Ljava/lang/String;", "getCdnUrl", "getCountryCode", "component1", "component2", "component3", "copy", "equals", MediaItem.DEFAULT_MEDIA_ID, "other", "hashCode", "toString", "write$Self", MediaItem.DEFAULT_MEDIA_ID, "self", "output", "Lkotlinx/serialization/encoding/CompositeEncoder;", "serialDesc", "Lkotlinx/serialization/descriptors/SerialDescriptor;", "write$Self$app_release", "$serializer", "Companion", "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
@Serializable
public final /* data */ class ApiReserveResponse {
    public static final int $stable = 0;

    /* JADX INFO: renamed from: Companion, reason: from kotlin metadata */
    public static final Companion INSTANCE = new Companion();
    private final String apiUrl;
    private final String cdnUrl;
    private final String countryCode;

    /* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
    @Metadata(d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001J\u0013\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00030\u0002¢\u0006\u0004\b\u0004\u0010\u0005¨\u0006\u0006"}, d2 = {"Lcom/skiy/lane/domain/model/ApiReserveResponse$Companion;", MediaItem.DEFAULT_MEDIA_ID, "Lkotlinx/serialization/KSerializer;", "Lcom/skiy/lane/domain/model/ApiReserveResponse;", "serializer", "()Lkotlinx/serialization/KSerializer;", "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
    public static final class Companion {
        public final KSerializer<ApiReserveResponse> serializer() {
            return ApiReserveResponse$$serializer.INSTANCE;
        }
    }

    public /* synthetic */ ApiReserveResponse(int i, String str, String str2, String str3, SerializationConstructorMarker serializationConstructorMarker) {
        if (7 != (i & 7)) {
            PluginExceptionsKt.a(i, 7, ApiReserveResponse$$serializer.INSTANCE.getDescriptor());
            throw null;
        }
        this.apiUrl = str;
        this.cdnUrl = str2;
        this.countryCode = str3;
    }

    public static /* synthetic */ ApiReserveResponse copy$default(ApiReserveResponse apiReserveResponse, String str, String str2, String str3, int i, Object obj) {
        if ((i & 1) != 0) {
            str = apiReserveResponse.apiUrl;
        }
        if ((i & 2) != 0) {
            str2 = apiReserveResponse.cdnUrl;
        }
        if ((i & 4) != 0) {
            str3 = apiReserveResponse.countryCode;
        }
        return apiReserveResponse.copy(str, str2, str3);
    }

    @JvmStatic
    public static final /* synthetic */ void write$Self$app_release(ApiReserveResponse self, CompositeEncoder output, SerialDescriptor serialDesc) {
        output.A(serialDesc, 0, self.apiUrl);
        output.A(serialDesc, 1, self.cdnUrl);
        output.A(serialDesc, 2, self.countryCode);
    }

    /* JADX INFO: renamed from: component1, reason: from getter */
    public final String getApiUrl() {
        return this.apiUrl;
    }

    /* JADX INFO: renamed from: component2, reason: from getter */
    public final String getCdnUrl() {
        return this.cdnUrl;
    }

    /* JADX INFO: renamed from: component3, reason: from getter */
    public final String getCountryCode() {
        return this.countryCode;
    }

    public final ApiReserveResponse copy(String apiUrl, String cdnUrl, String countryCode) {
        apiUrl.getClass();
        cdnUrl.getClass();
        countryCode.getClass();
        return new ApiReserveResponse(apiUrl, cdnUrl, countryCode);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ApiReserveResponse)) {
            return false;
        }
        ApiReserveResponse apiReserveResponse = (ApiReserveResponse) other;
        return Intrinsics.c(this.apiUrl, apiReserveResponse.apiUrl) && Intrinsics.c(this.cdnUrl, apiReserveResponse.cdnUrl) && Intrinsics.c(this.countryCode, apiReserveResponse.countryCode);
    }

    public final String getApiUrl() {
        return this.apiUrl;
    }

    public final String getCdnUrl() {
        return this.cdnUrl;
    }

    public final String getCountryCode() {
        return this.countryCode;
    }

    public int hashCode() {
        return this.countryCode.hashCode() + b60.c(this.apiUrl.hashCode() * 31, 31, this.cdnUrl);
    }

    public String toString() {
        String str = this.apiUrl;
        String str2 = this.cdnUrl;
        return w.m(b60.k("ApiReserveResponse(apiUrl=", str, ", cdnUrl=", str2, ", countryCode="), this.countryCode, ")");
    }

    public ApiReserveResponse(String str, String str2, String str3) {
        str.getClass();
        str2.getClass();
        str3.getClass();
        this.apiUrl = str;
        this.cdnUrl = str2;
        this.countryCode = str3;
    }
}
