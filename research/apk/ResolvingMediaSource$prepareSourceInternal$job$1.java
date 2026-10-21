package com.skiy.lane.app.playback;

import android.net.Uri;
import android.util.Log;
import androidx.media3.common.MediaItem;
import androidx.media3.datasource.TransferListener;
import androidx.media3.exoplayer.analytics.PlayerId;
import com.skiy.lane.data.local.UserCacheDataStore$special$$inlined$map$2;
import com.skiy.lane.data.local.UserCacheDataStore$special$$inlined$map$6;
import com.skiy.lane.data.utils.NetworkResult;
import com.skiy.lane.domain.BaseServerSetting;
import com.skiy.lane.domain.model.AudioQuality;
import com.skiy.lane.domain.model.TrackStreamingResult;
import com.skiy.lane.domain.model.User;
import defpackage.i;
import defpackage.w;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import kotlin.Metadata;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.CoroutineSingletons;
import kotlin.coroutines.jvm.internal.DebugMetadata;
import kotlin.coroutines.jvm.internal.SuspendLambda;
import kotlin.jvm.functions.Function2;
import kotlin.text.StringsKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.DelayKt;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.FlowKt__LimitKt$dropWhile$$inlined$unsafeFlow$1;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes.dex */
@Metadata(d1 = {"\u0000\n\n\u0000\n\u0002\u0010\u0002\n\u0002\u0018\u0002\u0010\u0000\u001a\u00020\u0001*\u00020\u0002H\n"}, d2 = {"<anonymous>", MediaItem.DEFAULT_MEDIA_ID, "Lkotlinx/coroutines/CoroutineScope;"}, k = 3, mv = {2, 2, 0}, xi = 48)
@DebugMetadata(c = "com.skiy.lane.app.playback.ResolvingMediaSource$prepareSourceInternal$job$1", f = "ResolvingMediaSource.kt", l = {118, 119, 128, 144, 152, 161, 182}, m = "invokeSuspend", v = 1)
final class ResolvingMediaSource$prepareSourceInternal$job$1 extends SuspendLambda implements Function2<CoroutineScope, Continuation<? super Unit>, Object> {
    public User a;
    public int b;
    public int c;
    public int d;
    public /* synthetic */ Object e;
    public final /* synthetic */ ResolvingMediaSource f;
    public final /* synthetic */ String l;
    public final /* synthetic */ TransferListener m;
    public final /* synthetic */ PlayerId n;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public ResolvingMediaSource$prepareSourceInternal$job$1(ResolvingMediaSource resolvingMediaSource, String str, TransferListener transferListener, PlayerId playerId, Continuation continuation) {
        super(2, continuation);
        this.f = resolvingMediaSource;
        this.l = str;
        this.m = transferListener;
        this.n = playerId;
    }

    @Override // kotlin.coroutines.jvm.internal.BaseContinuationImpl
    public final Continuation create(Object obj, Continuation continuation) {
        ResolvingMediaSource$prepareSourceInternal$job$1 resolvingMediaSource$prepareSourceInternal$job$1 = new ResolvingMediaSource$prepareSourceInternal$job$1(this.f, this.l, this.m, this.n, continuation);
        resolvingMediaSource$prepareSourceInternal$job$1.e = obj;
        return resolvingMediaSource$prepareSourceInternal$job$1;
    }

    @Override // kotlin.jvm.functions.Function2
    public final Object invoke(Object obj, Object obj2) {
        return ((ResolvingMediaSource$prepareSourceInternal$job$1) create((CoroutineScope) obj, (Continuation) obj2)).invokeSuspend(Unit.a);
    }

    /* JADX WARN: Can't wrap try/catch for region: R(5:45|112|46|(2:49|(1:51)(2:52|(1:54)(1:55)))(1:48)|(4:65|66|67|(1:69)(2:70|(3:72|(1:74)(1:77)|(2:84|85)(2:81|(1:83)))(2:86|(2:88|89)(2:90|91))))(2:60|(1:62))) */
    /* JADX WARN: Code duplicated, block: B:101:0x02e5  */
    /* JADX WARN: Code duplicated, block: B:104:0x02f4  */
    /* JADX WARN: Code duplicated, block: B:108:0x0328  */
    /* JADX WARN: Code duplicated, block: B:110:0x0330  */
    /* JADX WARN: Code duplicated, block: B:24:0x0061 A[Catch: Exception -> 0x0035, TRY_ENTER, TryCatch #1 {Exception -> 0x0035, blocks: (B:24:0x0061, B:27:0x0077, B:30:0x008f, B:32:0x0093, B:36:0x00a2, B:38:0x00aa, B:40:0x00ae, B:42:0x00bd, B:45:0x00c8, B:8:0x0031, B:13:0x003d, B:16:0x0049, B:19:0x0051), top: B:113:0x0015 }] */
    /* JADX WARN: Code duplicated, block: B:26:0x0075  */
    /* JADX WARN: Code duplicated, block: B:27:0x0077 A[Catch: Exception -> 0x0035, PHI: r0 r6 r7 r10 r12
  0x0077: PHI (r0v34 java.lang.Object) = (r0v40 java.lang.Object), (r0v41 java.lang.Object) binds: [B:25:0x0073, B:20:0x0054] A[DONT_GENERATE, DONT_INLINE]
  0x0077: PHI (r6v29 int) = (r6v30 int), (r6v34 int) binds: [B:25:0x0073, B:20:0x0054] A[DONT_GENERATE, DONT_INLINE]
  0x0077: PHI (r7v19 boolean) = (r7v20 boolean), (r7v26 boolean) binds: [B:25:0x0073, B:20:0x0054] A[DONT_GENERATE, DONT_INLINE]
  0x0077: PHI (r10v20 int) = (r10v21 int), (r10v0 int) binds: [B:25:0x0073, B:20:0x0054] A[DONT_GENERATE, DONT_INLINE]
  0x0077: PHI (r12v10 int) = (r12v11 int), (r12v14 int) binds: [B:25:0x0073, B:20:0x0054] A[DONT_GENERATE, DONT_INLINE], TryCatch #1 {Exception -> 0x0035, blocks: (B:24:0x0061, B:27:0x0077, B:30:0x008f, B:32:0x0093, B:36:0x00a2, B:38:0x00aa, B:40:0x00ae, B:42:0x00bd, B:45:0x00c8, B:8:0x0031, B:13:0x003d, B:16:0x0049, B:19:0x0051), top: B:113:0x0015 }] */
    /* JADX WARN: Code duplicated, block: B:29:0x008d  */
    /* JADX WARN: Code duplicated, block: B:30:0x008f A[Catch: Exception -> 0x0035, PHI: r0 r6 r7 r10 r12 r13
  0x008f: PHI (r0v24 com.skiy.lane.domain.model.User) = (r0v35 com.skiy.lane.domain.model.User), (r0v42 com.skiy.lane.domain.model.User) binds: [B:28:0x008b, B:17:0x004c] A[DONT_GENERATE, DONT_INLINE]
  0x008f: PHI (r6v13 int) = (r6v29 int), (r6v35 int) binds: [B:28:0x008b, B:17:0x004c] A[DONT_GENERATE, DONT_INLINE]
  0x008f: PHI (r7v3 boolean) = (r7v19 boolean), (r7v27 boolean) binds: [B:28:0x008b, B:17:0x004c] A[DONT_GENERATE, DONT_INLINE]
  0x008f: PHI (r10v13 int) = (r10v20 int), (r10v0 int) binds: [B:28:0x008b, B:17:0x004c] A[DONT_GENERATE, DONT_INLINE]
  0x008f: PHI (r12v9 int) = (r12v10 int), (r12v15 int) binds: [B:28:0x008b, B:17:0x004c] A[DONT_GENERATE, DONT_INLINE]
  0x008f: PHI (r13v1 java.lang.Object) = (r13v6 java.lang.Object), (r13v7 java.lang.Object) binds: [B:28:0x008b, B:17:0x004c] A[DONT_GENERATE, DONT_INLINE], TryCatch #1 {Exception -> 0x0035, blocks: (B:24:0x0061, B:27:0x0077, B:30:0x008f, B:32:0x0093, B:36:0x00a2, B:38:0x00aa, B:40:0x00ae, B:42:0x00bd, B:45:0x00c8, B:8:0x0031, B:13:0x003d, B:16:0x0049, B:19:0x0051), top: B:113:0x0015 }] */
    /* JADX WARN: Code duplicated, block: B:32:0x0093 A[Catch: Exception -> 0x0035, TryCatch #1 {Exception -> 0x0035, blocks: (B:24:0x0061, B:27:0x0077, B:30:0x008f, B:32:0x0093, B:36:0x00a2, B:38:0x00aa, B:40:0x00ae, B:42:0x00bd, B:45:0x00c8, B:8:0x0031, B:13:0x003d, B:16:0x0049, B:19:0x0051), top: B:113:0x0015 }] */
    /* JADX WARN: Code duplicated, block: B:35:0x00a1  */
    /* JADX WARN: Code duplicated, block: B:38:0x00aa A[Catch: Exception -> 0x0035, TryCatch #1 {Exception -> 0x0035, blocks: (B:24:0x0061, B:27:0x0077, B:30:0x008f, B:32:0x0093, B:36:0x00a2, B:38:0x00aa, B:40:0x00ae, B:42:0x00bd, B:45:0x00c8, B:8:0x0031, B:13:0x003d, B:16:0x0049, B:19:0x0051), top: B:113:0x0015 }] */
    /* JADX WARN: Code duplicated, block: B:72:0x01e7 A[Catch: Exception -> 0x01f4, TryCatch #2 {Exception -> 0x01f4, blocks: (B:67:0x01c6, B:70:0x01e1, B:72:0x01e7, B:74:0x01ef, B:79:0x01fa, B:81:0x0200, B:84:0x0252, B:85:0x0259, B:86:0x025a, B:88:0x025e, B:89:0x0278, B:90:0x0279, B:91:0x0280, B:92:0x0281, B:95:0x02b7, B:96:0x02bf), top: B:113:0x0015 }] */
    /* JADX WARN: Code duplicated, block: B:74:0x01ef A[Catch: Exception -> 0x01f4, TryCatch #2 {Exception -> 0x01f4, blocks: (B:67:0x01c6, B:70:0x01e1, B:72:0x01e7, B:74:0x01ef, B:79:0x01fa, B:81:0x0200, B:84:0x0252, B:85:0x0259, B:86:0x025a, B:88:0x025e, B:89:0x0278, B:90:0x0279, B:91:0x0280, B:92:0x0281, B:95:0x02b7, B:96:0x02bf), top: B:113:0x0015 }] */
    /* JADX WARN: Code duplicated, block: B:77:0x01f7  */
    /* JADX WARN: Code duplicated, block: B:86:0x025a A[Catch: Exception -> 0x01f4, TryCatch #2 {Exception -> 0x01f4, blocks: (B:67:0x01c6, B:70:0x01e1, B:72:0x01e7, B:74:0x01ef, B:79:0x01fa, B:81:0x0200, B:84:0x0252, B:85:0x0259, B:86:0x025a, B:88:0x025e, B:89:0x0278, B:90:0x0279, B:91:0x0280, B:92:0x0281, B:95:0x02b7, B:96:0x02bf), top: B:113:0x0015 }] */
    /* JADX WARN: Code duplicated, block: B:88:0x025e A[Catch: Exception -> 0x01f4, TryCatch #2 {Exception -> 0x01f4, blocks: (B:67:0x01c6, B:70:0x01e1, B:72:0x01e7, B:74:0x01ef, B:79:0x01fa, B:81:0x0200, B:84:0x0252, B:85:0x0259, B:86:0x025a, B:88:0x025e, B:89:0x0278, B:90:0x0279, B:91:0x0280, B:92:0x0281, B:95:0x02b7, B:96:0x02bf), top: B:113:0x0015 }] */
    /* JADX WARN: Code duplicated, block: B:90:0x0279 A[Catch: Exception -> 0x01f4, TryCatch #2 {Exception -> 0x01f4, blocks: (B:67:0x01c6, B:70:0x01e1, B:72:0x01e7, B:74:0x01ef, B:79:0x01fa, B:81:0x0200, B:84:0x0252, B:85:0x0259, B:86:0x025a, B:88:0x025e, B:89:0x0278, B:90:0x0279, B:91:0x0280, B:92:0x0281, B:95:0x02b7, B:96:0x02bf), top: B:113:0x0015 }] */
    /* JADX WARN: Code duplicated, block: B:99:0x02c4  */
    /* JADX WARN: Code restructure failed: missing block: B:63:0x0189, code lost:
    
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:64:0x018a, code lost:
    
        r7 = 2;
     */
    /* JADX WARN: Instruction removed from duplicated block: B:88:0x025e, please report this as an issue */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v17, types: [int] */
    /* JADX WARN: Type inference failed for: r0v25 */
    /* JADX WARN: Type inference failed for: r0v26, types: [int] */
    /* JADX WARN: Type inference failed for: r0v33 */
    /* JADX WARN: Type inference failed for: r0v45 */
    /* JADX WARN: Type inference failed for: r0v46 */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:108:0x0328 -> B:109:0x0329). Please report as a decompilation issue!!! */
    @Override // kotlin.coroutines.jvm.internal.BaseContinuationImpl
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public final Object invokeSuspend(Object obj) throws Exception {
        int i;
        int i2;
        int i3;
        long j;
        User user;
        Object objQ;
        ?? r0;
        NetworkResult networkResult;
        TrackStreamingResult trackStreamingResult;
        String url;
        Object objQ2;
        User user2;
        ?? r1;
        MediaItem.LocalConfiguration localConfiguration;
        Uri uri;
        Object objQ3;
        Unit unit = Unit.a;
        CoroutineScope coroutineScope = (CoroutineScope) this.e;
        CoroutineSingletons coroutineSingletons = CoroutineSingletons.a;
        int i4 = 2;
        i4 = 2;
        i4 = 2;
        int i5 = 0;
        z = false;
        z = false;
        boolean z = false;
        int i6 = 1;
        User user3 = null;
        try {
            try {
                switch (this.d) {
                    case 0:
                        ResultKt.b(obj);
                        i2 = 0;
                        if (CoroutineScopeKt.d(coroutineScope)) {
                            UserCacheDataStore$special$$inlined$map$2 userCacheDataStore$special$$inlined$map$2 = this.f.d.c;
                            this.e = coroutineScope;
                            this.a = user3;
                            this.b = i2;
                            this.d = i6;
                            objQ3 = FlowKt.q(userCacheDataStore$special$$inlined$map$2, this);
                            if (objQ3 == coroutineSingletons) {
                                user2 = (User) objQ3;
                                UserCacheDataStore$special$$inlined$map$6 userCacheDataStore$special$$inlined$map$6 = this.f.d.f;
                                this.e = coroutineScope;
                                this.a = user2;
                                this.b = i2;
                                this.d = i4;
                                objQ2 = FlowKt.q(userCacheDataStore$special$$inlined$map$6, this);
                                if (objQ2 != coroutineSingletons) {
                                    AudioQuality audioQuality = (AudioQuality) objQ2;
                                    if (user2 != null || user2.getPremiumExpiresIn() <= System.currentTimeMillis()) {
                                        r1 = z;
                                    } else {
                                        r1 = i6;
                                    }
                                    localConfiguration = this.f.b.localConfiguration;
                                    if (localConfiguration != null || (uri = localConfiguration.uri) == null) {
                                        throw new IOException("MediaItem does not contain a URI.");
                                    }
                                    String string = uri.toString();
                                    string.getClass();
                                    if (StringsKt.K(string, "http", z) || StringsKt.K(string, BaseServerSetting.b, z)) {
                                        Log.i("ResolvingMediaSource", "[" + this.f.b.mediaId + "] URI is a direct link. Playing.");
                                        ResolvingMediaSource resolvingMediaSource = this.f;
                                        TransferListener transferListener = this.m;
                                        PlayerId playerId = this.n;
                                        this.e = coroutineScope;
                                        this.a = null;
                                        this.b = i2;
                                        this.c = r1;
                                        this.d = 3;
                                        if (ResolvingMediaSource.a(uri, transferListener, playerId, resolvingMediaSource, this) == coroutineSingletons) {
                                        }
                                    } else {
                                        File file = new File(this.f.a.getFilesDir(), this.l + ".m4a");
                                        File file2 = new File(this.f.a.getFilesDir(), this.l + ".mp3");
                                        File file3 = new File(this.f.a.getFilesDir(), this.l + ".m3u8");
                                        if (!file.exists()) {
                                            if (file2.exists()) {
                                                file = file2;
                                            } else {
                                                file = file3.exists() ? file3 : null;
                                            }
                                        }
                                        if (file == null || !file.exists() || r1 == 0) {
                                            Log.d("ResolvingMediaSource", "[" + this.f.b.mediaId + "] Resolving stream URL (Attempt " + (i2 + 1) + ")...");
                                            FlowKt__LimitKt$dropWhile$$inlined$unsafeFlow$1 flowKt__LimitKt$dropWhile$$inlined$unsafeFlow$1 = new FlowKt__LimitKt$dropWhile$$inlined$unsafeFlow$1(new ResolvingMediaSource$prepareSourceInternal$job$1$streamResult$1(2, null), this.f.e.o(this.l, string, audioQuality.name()));
                                            this.e = coroutineScope;
                                            this.a = null;
                                            this.b = i2;
                                            this.c = r1;
                                            this.d = 5;
                                            objQ = FlowKt.q(flowKt__LimitKt$dropWhile$$inlined$unsafeFlow$1, this);
                                            r0 = r1;
                                            if (objQ != coroutineSingletons) {
                                                networkResult = (NetworkResult) objQ;
                                                if (!(networkResult instanceof NetworkResult.Success)) {
                                                    if (networkResult instanceof NetworkResult.Error) {
                                                        throw new IOException("Stream resolution ended unexpectedly.");
                                                    }
                                                    throw new IOException("API Error: " + ((NetworkResult.Error) networkResult).b);
                                                }
                                                trackStreamingResult = (TrackStreamingResult) ((NetworkResult.Success) networkResult).a;
                                                if (trackStreamingResult != null) {
                                                    url = trackStreamingResult.getUrl();
                                                } else {
                                                    url = null;
                                                }
                                                if (url != null || url.length() == 0) {
                                                    throw new IOException("Resolved URL is empty.");
                                                }
                                                Log.i("ResolvingMediaSource", "[" + this.f.b.mediaId + "] Successfully resolved URL: " + url);
                                                TrackStreamCacheManager trackStreamCacheManager = this.f.c;
                                                String str = this.l;
                                                trackStreamCacheManager.getClass();
                                                str.getClass();
                                                trackStreamingResult.getClass();
                                                trackStreamCacheManager.a.put(str, trackStreamingResult);
                                                ResolvingMediaSource resolvingMediaSource2 = this.f;
                                                TransferListener transferListener2 = this.m;
                                                PlayerId playerId2 = this.n;
                                                Uri uri2 = Uri.parse(url);
                                                this.e = coroutineScope;
                                                this.a = null;
                                                this.b = i2;
                                                this.c = r0;
                                                this.d = 6;
                                                if (ResolvingMediaSource.a(uri2, transferListener2, playerId2, resolvingMediaSource2, this) == coroutineSingletons) {
                                                }
                                            }
                                        } else {
                                            Log.i("ResolvingMediaSource", "[" + this.f.b.mediaId + "] Found local file and user is PREMIUM. Playing offline.");
                                            ResolvingMediaSource resolvingMediaSource3 = this.f;
                                            TransferListener transferListener3 = this.m;
                                            PlayerId playerId3 = this.n;
                                            Uri uriFromFile = Uri.fromFile(file);
                                            uriFromFile.getClass();
                                            this.e = coroutineScope;
                                            this.a = null;
                                            this.b = i2;
                                            this.c = r1;
                                            this.d = 4;
                                            if (ResolvingMediaSource.a(uriFromFile, transferListener3, playerId3, resolvingMediaSource3, this) == coroutineSingletons) {
                                            }
                                        }
                                    }
                                }
                            }
                            return coroutineSingletons;
                        }
                        return unit;
                    case 1:
                        i2 = this.b;
                        ResultKt.b(obj);
                        objQ3 = obj;
                        user2 = (User) objQ3;
                        UserCacheDataStore$special$$inlined$map$6 userCacheDataStore$special$$inlined$map$7 = this.f.d.f;
                        this.e = coroutineScope;
                        this.a = user2;
                        this.b = i2;
                        this.d = i4;
                        objQ2 = FlowKt.q(userCacheDataStore$special$$inlined$map$7, this);
                        if (objQ2 != coroutineSingletons) {
                            AudioQuality audioQuality2 = (AudioQuality) objQ2;
                            if (user2 != null) {
                                r1 = z;
                            } else {
                                r1 = z;
                            }
                            localConfiguration = this.f.b.localConfiguration;
                            if (localConfiguration != null) {
                                break;
                            }
                            throw new IOException("MediaItem does not contain a URI.");
                        }
                        return coroutineSingletons;
                    case 2:
                        i2 = this.b;
                        user2 = this.a;
                        ResultKt.b(obj);
                        objQ2 = obj;
                        AudioQuality audioQuality3 = (AudioQuality) objQ2;
                        if (user2 != null) {
                            r1 = z;
                        } else {
                            r1 = z;
                        }
                        localConfiguration = this.f.b.localConfiguration;
                        if (localConfiguration != null) {
                            break;
                        }
                        throw new IOException("MediaItem does not contain a URI.");
                    case 3:
                    case 4:
                    case 6:
                        int i7 = this.b;
                        ResultKt.b(obj);
                        return unit;
                    case 5:
                        int i8 = this.c;
                        i2 = this.b;
                        ResultKt.b(obj);
                        objQ = obj;
                        r0 = i8;
                        networkResult = (NetworkResult) objQ;
                        if (!(networkResult instanceof NetworkResult.Success)) {
                            trackStreamingResult = (TrackStreamingResult) ((NetworkResult.Success) networkResult).a;
                            if (trackStreamingResult != null) {
                                url = trackStreamingResult.getUrl();
                            } else {
                                url = null;
                            }
                            if (url != null) {
                                break;
                            }
                            throw new IOException("Resolved URL is empty.");
                        }
                        if (networkResult instanceof NetworkResult.Error) {
                            throw new IOException("Stream resolution ended unexpectedly.");
                        }
                        throw new IOException("API Error: " + ((NetworkResult.Error) networkResult).b);
                    case 7:
                        int i9 = this.b;
                        ResultKt.b(obj);
                        i2 = i9;
                        i5 = 2;
                        i3 = 1;
                        user = null;
                        user3 = user;
                        i4 = i5;
                        i6 = i3;
                        z = false;
                        if (CoroutineScopeKt.d(coroutineScope)) {
                            UserCacheDataStore$special$$inlined$map$2 userCacheDataStore$special$$inlined$map$3 = this.f.d.c;
                            this.e = coroutineScope;
                            this.a = user3;
                            this.b = i2;
                            this.d = i6;
                            objQ3 = FlowKt.q(userCacheDataStore$special$$inlined$map$3, this);
                            if (objQ3 == coroutineSingletons) {
                                user2 = (User) objQ3;
                                UserCacheDataStore$special$$inlined$map$6 userCacheDataStore$special$$inlined$map$8 = this.f.d.f;
                                this.e = coroutineScope;
                                this.a = user2;
                                this.b = i2;
                                this.d = i4;
                                objQ2 = FlowKt.q(userCacheDataStore$special$$inlined$map$8, this);
                                if (objQ2 != coroutineSingletons) {
                                    AudioQuality audioQuality4 = (AudioQuality) objQ2;
                                    if (user2 != null) {
                                        r1 = z;
                                    } else {
                                        r1 = z;
                                    }
                                    localConfiguration = this.f.b.localConfiguration;
                                    if (localConfiguration != null) {
                                        break;
                                    }
                                    throw new IOException("MediaItem does not contain a URI.");
                                }
                            }
                            return coroutineSingletons;
                        }
                        return unit;
                    default:
                        i.l("call to 'resume' before 'invoke' with coroutine");
                        return null;
                }
            } catch (Exception e) {
                e = e;
                i5 = 2;
                if (!(e instanceof CancellationException)) {
                    throw e;
                }
                i = i2 + 1;
                String str2 = this.f.b.mediaId;
                String message = e.getMessage();
                StringBuilder sbN = w.n(i, "[", str2, "] Resolution failed (Attempt ", "): ");
                sbN.append(message);
                Log.w("ResolvingMediaSource", sbN.toString());
                if (i2 > 3) {
                    i2 = 3;
                }
                i3 = 1;
                j = ((long) (1 << i2)) * 1000;
                if (j > 5000) {
                    j = 5000;
                }
                Log.d("ResolvingMediaSource", "[" + this.f.b.mediaId + "] Waiting " + j + "ms before retry...");
                this.e = coroutineScope;
                user = null;
                this.a = null;
                this.b = i;
                this.d = 7;
                if (DelayKt.b(j, this) != coroutineSingletons) {
                    i2 = i;
                }
                return coroutineSingletons;
            }
        } catch (Exception e2) {
            e = e2;
            if (!(e instanceof CancellationException)) {
                throw e;
            }
            i = i2 + 1;
            String str3 = this.f.b.mediaId;
            String message2 = e.getMessage();
            StringBuilder sbN2 = w.n(i, "[", str3, "] Resolution failed (Attempt ", "): ");
            sbN2.append(message2);
            Log.w("ResolvingMediaSource", sbN2.toString());
            if (i2 > 3) {
                i2 = 3;
            }
            i3 = 1;
            j = ((long) (1 << i2)) * 1000;
            if (j > 5000) {
                j = 5000;
            }
            Log.d("ResolvingMediaSource", "[" + this.f.b.mediaId + "] Waiting " + j + "ms before retry...");
            this.e = coroutineScope;
            user = null;
            this.a = null;
            this.b = i;
            this.d = 7;
            if (DelayKt.b(j, this) != coroutineSingletons) {
                i2 = i;
            }
            return coroutineSingletons;
        }
    }
}
