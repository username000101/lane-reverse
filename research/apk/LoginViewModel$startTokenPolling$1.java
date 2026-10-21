package com.skiy.lane.presentation.compose.login;

import android.content.Context;
import android.provider.Settings;
import androidx.media3.common.MediaItem;
import com.skiy.lane.domain.model.LaneTokenResponse;
import com.skiy.lane.domain.repository.AuthRepository;
import defpackage.i;
import kotlin.Metadata;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.CoroutineSingletons;
import kotlin.coroutines.jvm.internal.DebugMetadata;
import kotlin.coroutines.jvm.internal.SuspendLambda;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.MutableStateFlow;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes3.dex */
@Metadata(d1 = {"\u0000\n\n\u0000\n\u0002\u0010\u0002\n\u0002\u0018\u0002\u0010\u0000\u001a\u00020\u0001*\u00020\u0002H\n"}, d2 = {"<anonymous>", MediaItem.DEFAULT_MEDIA_ID, "Lkotlinx/coroutines/CoroutineScope;"}, k = 3, mv = {2, 2, 0}, xi = 48)
@DebugMetadata(c = "com.skiy.lane.presentation.compose.login.LoginViewModel$startTokenPolling$1", f = "LoginViewModel.kt", l = {111}, m = "invokeSuspend", v = 1)
final class LoginViewModel$startTokenPolling$1 extends SuspendLambda implements Function2<CoroutineScope, Continuation<? super Unit>, Object> {
    public int a;
    public final /* synthetic */ LoginViewModel b;
    public final /* synthetic */ Context c;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public LoginViewModel$startTokenPolling$1(LoginViewModel loginViewModel, Context context, Continuation continuation) {
        super(2, continuation);
        this.b = loginViewModel;
        this.c = context;
    }

    @Override // kotlin.coroutines.jvm.internal.BaseContinuationImpl
    public final Continuation create(Object obj, Continuation continuation) {
        return new LoginViewModel$startTokenPolling$1(this.b, this.c, continuation);
    }

    @Override // kotlin.jvm.functions.Function2
    public final Object invoke(Object obj, Object obj2) {
        return ((LoginViewModel$startTokenPolling$1) create((CoroutineScope) obj, (Continuation) obj2)).invokeSuspend(Unit.a);
    }

    @Override // kotlin.coroutines.jvm.internal.BaseContinuationImpl
    public final Object invokeSuspend(Object obj) throws Throwable {
        Object objA;
        LoginViewModel loginViewModel = this.b;
        MutableStateFlow mutableStateFlow = loginViewModel.e;
        CoroutineSingletons coroutineSingletons = CoroutineSingletons.a;
        int i = this.a;
        if (i == 0) {
            ResultKt.b(obj);
            mutableStateFlow.setValue(LoginUiState.Loading.a);
            Context context = this.c;
            context.getClass();
            String string = Settings.Secure.getString(context.getContentResolver(), "android_id");
            string.getClass();
            AuthRepository authRepository = loginViewModel.b;
            this.a = 1;
            objA = authRepository.a(string, this);
            if (objA == coroutineSingletons) {
                return coroutineSingletons;
            }
        } else {
            if (i != 1) {
                i.l("call to 'resume' before 'invoke' with coroutine");
                return null;
            }
            ResultKt.b(obj);
            objA = ((Result) obj).a;
        }
        if (!(objA instanceof Result.Failure)) {
            mutableStateFlow.setValue(new LoginUiState.Success((LaneTokenResponse) objA));
        }
        Throwable thA = Result.a(objA);
        if (thA != null) {
            String message = thA.getMessage();
            if (message == null) {
                message = "Произошла неизвестная ошибка";
            }
            mutableStateFlow.setValue(new LoginUiState.Error(message));
        }
        return Unit.a;
    }
}
