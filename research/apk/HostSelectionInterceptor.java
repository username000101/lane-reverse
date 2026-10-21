package com.skiy.lane.app.network;

import com.skiy.lane.domain.BaseServerSetting;
import javax.inject.Singleton;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.RealInterceptorChain;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes3.dex */
@Singleton
@Metadata(d1 = {"\u0000\n\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001¨\u0006\u0002"}, d2 = {"Lcom/skiy/lane/app/network/HostSelectionInterceptor;", "Lokhttp3/Interceptor;", "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class HostSelectionInterceptor implements Interceptor {
    @Override // okhttp3.Interceptor
    public final Response a(RealInterceptorChain realInterceptorChain) {
        HttpUrl httpUrlB;
        Request request = realInterceptorChain.e;
        String str = BaseServerSetting.a;
        HttpUrl.INSTANCE.getClass();
        str.getClass();
        try {
            httpUrlB = HttpUrl.Companion.b(str);
        } catch (IllegalArgumentException unused) {
            httpUrlB = null;
        }
        if (httpUrlB == null || Intrinsics.c(request.a.d, httpUrlB.d) || StringsKt.j(request.a.d, "laneaudiobucket", false) || StringsKt.j(request.a.d, "sndcdn.com", false)) {
            return realInterceptorChain.b(request);
        }
        HttpUrl.Builder builderF = request.a.f();
        builderF.f(httpUrlB.a);
        builderF.c(httpUrlB.d);
        builderF.e(httpUrlB.e);
        HttpUrl httpUrlB2 = builderF.b();
        Request.Builder builderA = request.a();
        builderA.a = httpUrlB2;
        return realInterceptorChain.b(new Request(builderA));
    }
}
