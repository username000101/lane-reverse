package com.skiy.lane.data.repository;

import com.skiy.lane.data.remote.api.TracksApi;
import com.skiy.lane.domain.model.TrackStreamingResult;
import defpackage.i;
import kotlin.Metadata;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.CoroutineSingletons;
import kotlin.coroutines.jvm.internal.DebugMetadata;
import kotlin.coroutines.jvm.internal.SuspendLambda;
import kotlin.jvm.functions.Function1;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes.dex */
@Metadata(d1 = {"\u0000\u0006\n\u0000\n\u0002\u0018\u0002\u0010\u0000\u001a\u00020\u0001H\n"}, d2 = {"<anonymous>", "Lcom/skiy/lane/domain/model/TrackStreamingResult;"}, k = 3, mv = {2, 2, 0}, xi = 48)
@DebugMetadata(c = "com.skiy.lane.data.repository.TrackRepositoryImpl$getGenericStream$1", f = "TrackRepositoryImpl.kt", l = {32}, m = "invokeSuspend", v = 1)
final class TrackRepositoryImpl$getGenericStream$1 extends SuspendLambda implements Function1<Continuation<? super TrackStreamingResult>, Object> {
    public int a;
    public final /* synthetic */ TrackRepositoryImpl b;
    public final /* synthetic */ String c;
    public final /* synthetic */ String d;
    public final /* synthetic */ String e;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public TrackRepositoryImpl$getGenericStream$1(TrackRepositoryImpl trackRepositoryImpl, String str, String str2, String str3, Continuation continuation) {
        super(1, continuation);
        this.b = trackRepositoryImpl;
        this.c = str;
        this.d = str2;
        this.e = str3;
    }

    @Override // kotlin.coroutines.jvm.internal.BaseContinuationImpl
    public final Continuation create(Continuation continuation) {
        return new TrackRepositoryImpl$getGenericStream$1(this.b, this.c, this.d, this.e, continuation);
    }

    @Override // kotlin.jvm.functions.Function1
    public final Object invoke(Object obj) {
        return ((TrackRepositoryImpl$getGenericStream$1) create((Continuation) obj)).invokeSuspend(Unit.a);
    }

    @Override // kotlin.coroutines.jvm.internal.BaseContinuationImpl
    public final Object invokeSuspend(Object obj) throws Throwable {
        CoroutineSingletons coroutineSingletons = CoroutineSingletons.a;
        int i = this.a;
        if (i != 0) {
            if (i == 1) {
                ResultKt.b(obj);
                return obj;
            }
            i.l("call to 'resume' before 'invoke' with coroutine");
            return null;
        }
        ResultKt.b(obj);
        TracksApi tracksApi = this.b.a;
        this.a = 1;
        Object objI = tracksApi.i(this.c, this.d, this.e, this);
        return objI == coroutineSingletons ? coroutineSingletons : objI;
    }
}
