package com.skiy.lane.data.repository;

import androidx.datastore.preferences.protobuf.DescriptorProtos;
import androidx.media3.common.C;
import coil3.network.internal.UtilsKt;
import com.skiy.lane.domain.BaseServerSetting;
import com.skiy.lane.domain.model.LaneTokenResponse;
import defpackage.el;
import defpackage.i;
import java.io.IOException;
import kotlin.Metadata;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.SafeContinuation;
import kotlin.coroutines.intrinsics.CoroutineSingletons;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.coroutines.jvm.internal.DebugMetadata;
import kotlin.coroutines.jvm.internal.SuspendLambda;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.DelayKt;
import kotlinx.serialization.json.Json;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.connection.RealCall;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes3.dex */
@Metadata(d1 = {"\u0000\n\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\u0010\u0000\u001a\u00020\u0001*\u00020\u0002H\n"}, d2 = {"<anonymous>", "Lcom/skiy/lane/domain/model/LaneTokenResponse;", "Lkotlinx/coroutines/CoroutineScope;"}, k = 3, mv = {2, 2, 0}, xi = 48)
@DebugMetadata(c = "com.skiy.lane.data.repository.AuthRepositoryImpl$pollForToken$2$1", f = "AuthRepositoryImpl.kt", l = {DescriptorProtos.FileOptions.PHP_CLASS_PREFIX_FIELD_NUMBER, 64}, m = "invokeSuspend", v = 1)
final class AuthRepositoryImpl$pollForToken$2$1 extends SuspendLambda implements Function2<CoroutineScope, Continuation<? super LaneTokenResponse>, Object> {
    public AuthRepositoryImpl a;
    public int b;
    public int c;
    public /* synthetic */ Object d;
    public final /* synthetic */ AuthRepositoryImpl e;
    public final /* synthetic */ String f;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public AuthRepositoryImpl$pollForToken$2$1(AuthRepositoryImpl authRepositoryImpl, String str, Continuation continuation) {
        super(2, continuation);
        this.e = authRepositoryImpl;
        this.f = str;
    }

    @Override // kotlin.coroutines.jvm.internal.BaseContinuationImpl
    public final Continuation create(Object obj, Continuation continuation) {
        AuthRepositoryImpl$pollForToken$2$1 authRepositoryImpl$pollForToken$2$1 = new AuthRepositoryImpl$pollForToken$2$1(this.e, this.f, continuation);
        authRepositoryImpl$pollForToken$2$1.d = obj;
        return authRepositoryImpl$pollForToken$2$1;
    }

    @Override // kotlin.jvm.functions.Function2
    public final Object invoke(Object obj, Object obj2) {
        return ((AuthRepositoryImpl$pollForToken$2$1) create((CoroutineScope) obj, (Continuation) obj2)).invokeSuspend(Unit.a);
    }

    /* JADX WARN: Code duplicated, block: B:20:0x0094  */
    /* JADX WARN: Code duplicated, block: B:22:0x00a8  */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:23:0x00b7 -> B:11:0x002c). Please report as a decompilation issue!!! */
    @Override // kotlin.coroutines.jvm.internal.BaseContinuationImpl
    public final Object invokeSuspend(Object obj) throws Throwable {
        int i;
        String str;
        AuthRepositoryImpl authRepositoryImpl;
        CoroutineScope coroutineScope = (CoroutineScope) this.d;
        CoroutineSingletons coroutineSingletons = CoroutineSingletons.a;
        int i2 = this.c;
        if (i2 == 0) {
            ResultKt.b(obj);
            i = 0;
        } else {
            if (i2 == 1) {
                i = this.b;
                ResultKt.b(obj);
                str = (String) obj;
                authRepositoryImpl = this.e;
                if (str != null) {
                    Json json = authRepositoryImpl.b;
                    json.getClass();
                    return (LaneTokenResponse) json.b(LaneTokenResponse.INSTANCE.serializer(), str);
                }
                i++;
                this.d = coroutineScope;
                this.a = null;
                this.b = i;
                this.c = 2;
                if (DelayKt.b(C.DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS, this) != coroutineSingletons) {
                }
                return coroutineSingletons;
            }
            if (i2 != 2) {
                i.l("call to 'resume' before 'invoke' with coroutine");
                return null;
            }
            i = this.b;
            ResultKt.b(obj);
        }
        if (CoroutineScopeKt.d(coroutineScope) || i >= 20) {
            el.q("Не удалось получить токен авторизации. Попробуйте снова.");
            return null;
        }
        Request.Builder builder = new Request.Builder();
        builder.d(BaseServerSetting.a + "/auth/" + this.f);
        builder.c(UtilsKt.HTTP_METHOD_GET, null);
        Request request = new Request(builder);
        AuthRepositoryImpl authRepositoryImpl2 = this.e;
        this.d = coroutineScope;
        this.a = authRepositoryImpl2;
        this.b = i;
        this.c = 1;
        final SafeContinuation safeContinuation = new SafeContinuation(IntrinsicsKt.b(this), CoroutineSingletons.b);
        OkHttpClient okHttpClient = authRepositoryImpl2.a;
        okHttpClient.getClass();
        new RealCall(okHttpClient, request).s(new Callback() { // from class: com.skiy.lane.data.repository.AuthRepositoryImpl$pollForToken$2$1$responseBody$1$1
            @Override // okhttp3.Callback
            public final void onFailure(Call call, IOException iOException) {
                if (call.getV()) {
                    return;
                }
                safeContinuation.resumeWith(new Result.Failure(iOException));
            }

            @Override // okhttp3.Callback
            public final void onResponse(Call call, Response response) {
                boolean z = response.u;
                SafeContinuation safeContinuation2 = safeContinuation;
                if (z) {
                    safeContinuation2.resumeWith(response.l.d());
                } else {
                    safeContinuation2.resumeWith(null);
                }
            }
        });
        obj = safeContinuation.a();
        if (obj != coroutineSingletons) {
            str = (String) obj;
            authRepositoryImpl = this.e;
            if (str != null) {
                Json json2 = authRepositoryImpl.b;
                json2.getClass();
                return (LaneTokenResponse) json2.b(LaneTokenResponse.INSTANCE.serializer(), str);
            }
            i++;
            this.d = coroutineScope;
            this.a = null;
            this.b = i;
            this.c = 2;
            if (DelayKt.b(C.DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS, this) != coroutineSingletons) {
                if (CoroutineScopeKt.d(coroutineScope)) {
                }
                el.q("Не удалось получить токен авторизации. Попробуйте снова.");
                return null;
            }
        }
        return coroutineSingletons;
    }
}
