package com.skiy.lane.app.network;

import com.skiy.lane.app.utils.LDIProvider;
import com.skiy.lane.domain.BaseServerSetting;
import java.util.Locale;
import java.util.TimeZone;
import javax.inject.Singleton;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.RealInterceptorChain;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes.dex */
@Singleton
@Metadata(d1 = {"\u0000\n\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001¨\u0006\u0002"}, d2 = {"Lcom/skiy/lane/app/network/ServiceInfoInterceptor;", "Lokhttp3/Interceptor;", "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class ServiceInfoInterceptor implements Interceptor {
    public final LDIProvider a;

    public ServiceInfoInterceptor(LDIProvider lDIProvider) {
        lDIProvider.getClass();
        this.a = lDIProvider;
    }

    @Override // okhttp3.Interceptor
    public final Response a(RealInterceptorChain realInterceptorChain) {
        Request request = realInterceptorChain.e;
        if (!StringsKt.j(request.a.i, BaseServerSetting.a, false)) {
            return realInterceptorChain.b(request);
        }
        String language = Locale.getDefault().getLanguage();
        String id = TimeZone.getDefault().getID();
        if (Intrinsics.c(id, "Europe/Kiev")) {
            id = "Europe/Kyiv";
        }
        String str = (String) this.a.b.getValue();
        String str2 = (this.a.a.getResources().getConfiguration().uiMode & 48) == 32 ? "dark" : "light";
        Request.Builder builderA = realInterceptorChain.e.a();
        language.getClass();
        builderA.a("Accept-Language", language);
        builderA.a("LDI", str);
        id.getClass();
        builderA.a("TZ", id);
        builderA.a("X-App-Version", "207");
        builderA.a("X-Platform", "android");
        builderA.a("X-Theme", str2);
        builderA.b("User-Agent", "LaneMusic/1.0 (Android; Mobile)");
        return realInterceptorChain.b(new Request(builderA));
    }
}
