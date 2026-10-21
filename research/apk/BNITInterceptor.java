package com.skiy.lane.app.network;

import coil3.disk.DiskLruCache;
import coil3.network.internal.UtilsKt;
import com.skiy.lane.app.BNITManager;
import com.skiy.lane.app.utils.TimeCorrectionManager;
import com.skiy.lane.domain.BaseServerSetting;
import defpackage.c;
import defpackage.el;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import javax.inject.Singleton;
import kotlin.Metadata;
import kotlin.Pair;
import kotlin.collections.CollectionsKt;
import kotlin.collections.EmptySet;
import kotlin.io.CloseableKt;
import kotlin.io.TextStreamsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.IntProgression;
import kotlin.ranges.RangesKt;
import kotlin.text.Charsets;
import kotlin.text.StringsKt;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.ResponseBody$Companion$asResponseBody$1;
import okhttp3.internal.Internal;
import okhttp3.internal._HeadersCommonKt;
import okhttp3.internal.http.RealInterceptorChain;
import okio.Buffer;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes3.dex */
@Singleton
@Metadata(d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u0000 \u00022\u00020\u0001:\u0001\u0002¨\u0006\u0003"}, d2 = {"Lcom/skiy/lane/app/network/BNITInterceptor;", "Lokhttp3/Interceptor;", "Companion", "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class BNITInterceptor implements Interceptor {
    public final TimeCorrectionManager a;

    public BNITInterceptor(TimeCorrectionManager timeCorrectionManager) {
        timeCorrectionManager.getClass();
        this.a = timeCorrectionManager;
    }

    @Override // okhttp3.Interceptor
    public final Response a(RealInterceptorChain realInterceptorChain) throws IOException {
        Iterable iterableUnmodifiableSet;
        byte[] bArrR;
        ResponseBody responseBody;
        String str;
        String str2;
        int iS;
        RequestBody requestBody;
        Request request = realInterceptorChain.e;
        if (!StringsKt.j(request.a.i, BaseServerSetting.a, false)) {
            return realInterceptorChain.b(request);
        }
        HttpUrl httpUrl = request.a;
        String upperCase = request.b.toUpperCase(Locale.ROOT);
        upperCase.getClass();
        String strB = httpUrl.b();
        List list = httpUrl.g;
        if (list == null) {
            iterableUnmodifiableSet = EmptySet.a;
        } else {
            LinkedHashSet linkedHashSet = new LinkedHashSet(list.size() / 2, 1.0f);
            IntProgression intProgressionI = RangesKt.i(RangesKt.j(0, list.size()), 2);
            int i = intProgressionI.a;
            int i2 = intProgressionI.b;
            int i3 = intProgressionI.c;
            if ((i3 > 0 && i <= i2) || (i3 < 0 && i2 <= i)) {
                while (true) {
                    Object obj = list.get(i);
                    obj.getClass();
                    linkedHashSet.add(obj);
                    if (i == i2) {
                        break;
                    }
                    i += i3;
                }
            }
            iterableUnmodifiableSet = Collections.unmodifiableSet(linkedHashSet);
            iterableUnmodifiableSet.getClass();
        }
        String strF = CollectionsKt.F(CollectionsKt.b0(iterableUnmodifiableSet), "&", null, null, new c(httpUrl, 5), 30);
        RequestBody requestBody2 = request.d;
        MediaType mediaTypeA = null;
        MediaType a = requestBody2 != null ? requestBody2.getA() : null;
        boolean zC = Intrinsics.c(a != null ? a.b : null, "multipart");
        if (zC || (requestBody = request.d) == null) {
            bArrR = new byte[0];
        } else {
            Buffer buffer = new Buffer();
            requestBody.d(buffer);
            bArrR = buffer.r(buffer.b);
        }
        TimeCorrectionManager timeCorrectionManager = this.a;
        timeCorrectionManager.getClass();
        byte[][] bArrCdrl = BNITManager.cdrl(upperCase, strB, strF, bArrR, (System.currentTimeMillis() + timeCorrectionManager.a) - System.currentTimeMillis());
        if (bArrCdrl.length == 0) {
            el.q("SCF Error");
            return null;
        }
        Request.Builder builderA = request.a();
        builderA.b("X-Accept-Red", DiskLruCache.VERSION);
        for (int i4 = 0; i4 < 3; i4++) {
            if (i4 < bArrCdrl.length && (iS = StringsKt.s((str2 = new String(bArrCdrl[i4], Charsets.a)), ":", 0, false, 6)) > 0) {
                builderA.b(StringsKt.X(str2.substring(0, iS)).toString(), StringsKt.X(str2.substring(iS + 1)).toString());
            }
        }
        if (bArrCdrl.length > 3 && !zC) {
            byte[] bArr = bArrCdrl[3];
            if (bArr.length != 0) {
                if (a == null) {
                    MediaType.INSTANCE.getClass();
                    try {
                        a = MediaType.Companion.a("application/json; charset=utf-8");
                    } catch (IllegalArgumentException unused) {
                        a = null;
                    }
                }
                RequestBody.Companion companion = RequestBody.INSTANCE;
                int length = bArr.length;
                companion.getClass();
                builderA.c(upperCase, RequestBody.Companion.a(length, a, bArr));
            }
        }
        Response responseB = realInterceptorChain.b(new Request(builderA));
        boolean zC2 = Intrinsics.c(Response.a("X-Core-Red", responseB), DiskLruCache.VERSION);
        boolean zC3 = Intrinsics.c(Response.a("X-Core-Compressed", responseB), DiskLruCache.VERSION);
        String strA = Response.a("X-Resp-Nonce", responseB);
        if (!zC2 || strA == null || StringsKt.t(strA) || (responseBody = responseB.l) == null) {
            return responseB;
        }
        try {
            byte[] bArrVerifyMagic = BNITManager.verifyMagic(responseBody.a(), strA);
            if (zC3) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(bArrVerifyMagic)), Charsets.a), 8192);
                try {
                    str = TextStreamsKt.a(bufferedReader);
                    bufferedReader.close();
                } catch (Throwable th) {
                    try {
                        throw th;
                    } catch (Throwable th2) {
                        CloseableKt.a(bufferedReader, th);
                        throw th2;
                    }
                }
            } else {
                str = new String(bArrVerifyMagic, Charsets.a);
            }
            MediaType.INSTANCE.getClass();
            try {
                mediaTypeA = MediaType.Companion.a("application/json; charset=utf-8");
            } catch (IllegalArgumentException unused2) {
            }
            ResponseBody.INSTANCE.getClass();
            Pair pairA = Internal.a(mediaTypeA);
            Charset charset = (Charset) pairA.a;
            MediaType mediaType = (MediaType) pairA.b;
            Buffer buffer2 = new Buffer();
            charset.getClass();
            buffer2.j0(str, 0, str.length(), charset);
            ResponseBody$Companion$asResponseBody$1 responseBody$Companion$asResponseBody$1 = new ResponseBody$Companion$asResponseBody$1(mediaType, buffer2.b, buffer2);
            Response.Builder builderB = responseB.b();
            builderB.g = responseBody$Companion$asResponseBody$1;
            Headers.Builder builder = builderB.f;
            builder.getClass();
            _HeadersCommonKt.b(UtilsKt.CONTENT_TYPE);
            _HeadersCommonKt.c("application/json; charset=utf-8", UtilsKt.CONTENT_TYPE);
            builder.d(UtilsKt.CONTENT_TYPE);
            _HeadersCommonKt.a(builder, UtilsKt.CONTENT_TYPE, "application/json; charset=utf-8");
            builderB.f.d("Content-Length");
            builderB.f.d("Content-Encoding");
            return builderB.a();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Decryption/Decompression failed", e);
        }
    }
}
