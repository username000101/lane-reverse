package com.skiy.lane.presentation.compose.login;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelKt;
import com.skiy.lane.domain.repository.AuthRepository;
import com.skiy.lane.domain.repository.UserRepository;
import kotlin.Metadata;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import kotlinx.serialization.json.Json;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes3.dex */
@Metadata(d1 = {"\u0000\n\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001¨\u0006\u0002"}, d2 = {"Lcom/skiy/lane/presentation/compose/login/LoginViewModel;", "Landroidx/lifecycle/ViewModel;", "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class LoginViewModel extends ViewModel {
    public final AuthRepository b;
    public final UserRepository c;
    public final Json d;
    public final MutableStateFlow e;
    public final StateFlow f;

    public LoginViewModel(AuthRepository authRepository, UserRepository userRepository, Json json) {
        authRepository.getClass();
        userRepository.getClass();
        json.getClass();
        this.b = authRepository;
        this.c = userRepository;
        this.d = json;
        MutableStateFlow mutableStateFlowA = StateFlowKt.a(LoginUiState.Idle.a);
        this.e = mutableStateFlowA;
        this.f = FlowKt.c(mutableStateFlowA);
    }

    public final void f(String str) {
        str.getClass();
        BuildersKt.c(ViewModelKt.a(this), null, new LoginViewModel$signInWithGoogle$1(this, str, null), 3);
    }
}
