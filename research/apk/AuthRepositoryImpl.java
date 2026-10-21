package com.skiy.lane.data.repository;

import com.skiy.lane.domain.model.LaneTokenResponse;
import com.skiy.lane.domain.repository.AuthRepository;
import defpackage.i;
import defpackage.p;
import kotlin.Metadata;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.coroutines.intrinsics.CoroutineSingletons;
import kotlin.coroutines.jvm.internal.ContinuationImpl;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.serialization.json.Json;
import kotlinx.serialization.json.JsonKt;
import okhttp3.OkHttpClient;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes.dex */
@Metadata(d1 = {"\u0000\n\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001¨\u0006\u0002"}, d2 = {"Lcom/skiy/lane/data/repository/AuthRepositoryImpl;", "Lcom/skiy/lane/domain/repository/AuthRepository;", "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class AuthRepositoryImpl implements AuthRepository {
    public final OkHttpClient a;
    public final Json b;

    public AuthRepositoryImpl(OkHttpClient okHttpClient) {
        okHttpClient.getClass();
        this.a = okHttpClient;
        this.b = JsonKt.a(new p(25));
    }

    /* JADX WARN: Code duplicated, block: B:7:0x0013  */
    @Override // com.skiy.lane.domain.repository.AuthRepository
    public final Object a(String str, ContinuationImpl continuationImpl) {
        AuthRepositoryImpl$pollForToken$1 authRepositoryImpl$pollForToken$1;
        if (continuationImpl instanceof AuthRepositoryImpl$pollForToken$1) {
            authRepositoryImpl$pollForToken$1 = (AuthRepositoryImpl$pollForToken$1) continuationImpl;
            int i = authRepositoryImpl$pollForToken$1.c;
            if ((i & Integer.MIN_VALUE) != 0) {
                authRepositoryImpl$pollForToken$1.c = i - Integer.MIN_VALUE;
            } else {
                authRepositoryImpl$pollForToken$1 = new AuthRepositoryImpl$pollForToken$1(this, continuationImpl);
            }
        } else {
            authRepositoryImpl$pollForToken$1 = new AuthRepositoryImpl$pollForToken$1(this, continuationImpl);
        }
        Object objC = authRepositoryImpl$pollForToken$1.a;
        CoroutineSingletons coroutineSingletons = CoroutineSingletons.a;
        int i2 = authRepositoryImpl$pollForToken$1.c;
        try {
            if (i2 == 0) {
                ResultKt.b(objC);
                AuthRepositoryImpl$pollForToken$2$1 authRepositoryImpl$pollForToken$2$1 = new AuthRepositoryImpl$pollForToken$2$1(this, str, null);
                authRepositoryImpl$pollForToken$1.c = 1;
                objC = CoroutineScopeKt.c(authRepositoryImpl$pollForToken$2$1, authRepositoryImpl$pollForToken$1);
                if (objC == coroutineSingletons) {
                    return coroutineSingletons;
                }
            } else {
                if (i2 != 1) {
                    i.l("call to 'resume' before 'invoke' with coroutine");
                    return null;
                }
                ResultKt.b(objC);
            }
            return (LaneTokenResponse) objC;
        } catch (Throwable th) {
            return new Result.Failure(th);
        }
    }
}
