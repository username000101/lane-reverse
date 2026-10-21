package com.skiy.lane.app.network;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist;
import com.skiy.lane.data.local.TokenManager;
import com.skiy.lane.domain.BaseServerSetting;
import javax.inject.Singleton;
import kotlin.Metadata;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;
import kotlinx.coroutines.BuildersKt;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.RealInterceptorChain;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes.dex */
@Singleton
@Metadata(d1 = {"\u0000\n\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001¨\u0006\u0002"}, d2 = {"Lcom/skiy/lane/app/network/AuthInterceptor;", "Lokhttp3/Interceptor;", "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class AuthInterceptor implements Interceptor {
    public final TokenManager a;

    public AuthInterceptor(TokenManager tokenManager) {
        tokenManager.getClass();
        this.a = tokenManager;
    }

    @Override // okhttp3.Interceptor
    public final Response a(RealInterceptorChain realInterceptorChain) {
        Request request = realInterceptorChain.e;
        if (!StringsKt.j(request.a.i, BaseServerSetting.a, false)) {
            return realInterceptorChain.b(request);
        }
        if (Intrinsics.c(request.b, HlsMediaPlaylist.Interstitial.CUE_TRIGGER_POST) && StringsKt.n(request.a.b(), "/createUserOrLogin", false)) {
            return realInterceptorChain.b(request);
        }
        Request.Builder builderA = request.a();
        String str = (String) BuildersKt.d(EmptyCoroutineContext.a, new AuthInterceptor$intercept$tokenProvider$1(this, null));
        if (str != null) {
            builderA.a("Authorization", "Bearer ".concat(str));
            builderA.a("X-App-Version", MediaItem.DEFAULT_MEDIA_ID);
        }
        return realInterceptorChain.b(new Request(builderA));
    }
}
