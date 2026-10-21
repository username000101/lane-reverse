package com.skiy.lane.app.playback;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.skiy.lane.app.events.AppEvents;
import com.skiy.lane.app.utils.LaneLogger;
import com.skiy.lane.domain.model.DeezerAdditionalData;
import com.skiy.lane.domain.model.SoundCloudAdditionalData;
import com.skiy.lane.domain.model.SpotifyAdditionalData;
import com.skiy.lane.domain.model.TelegramAdditionalData;
import com.skiy.lane.domain.model.TrackData;
import com.skiy.lane.domain.repository.PlaylistRepository;
import com.skiy.lane.domain.repository.TrackRepository;
import com.skiy.lane.domain.repository.UserRepository;
import defpackage.co;
import defpackage.i;
import defpackage.iq;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Singleton;
import kotlin.Metadata;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.collections.EmptyList;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.CoroutineSingletons;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.Job;
import kotlinx.coroutines.JobSupport;
import kotlinx.coroutines.MainCoroutineDispatcher;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import kotlinx.coroutines.internal.MainDispatcherLoader;
import kotlinx.coroutines.scheduling.DefaultScheduler;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes.dex */
@Singleton
@Metadata(d1 = {"\u0000\n\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\b\u0007\u0018\u00002\u00020\u0001¨\u0006\u0002"}, d2 = {"Lcom/skiy/lane/app/playback/MusicPlayerController;", MediaItem.DEFAULT_MEDIA_ID, "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class MusicPlayerController {
    public final CoroutineScope a;
    public final UserRepository b;
    public final TrackRepository c;
    public final PlaylistRepository d;
    public final TrackStreamCacheManager e;
    public final AppEvents f;
    public final Cache g;
    public final ListenableFuture h;
    public MediaController i;
    public final MutableStateFlow j;
    public final StateFlow k;
    public final MutableStateFlow l;
    public final StateFlow m;
    public final MutableStateFlow n;
    public final StateFlow o;
    public final MutableStateFlow p;
    public final StateFlow q;
    public final MutableStateFlow r;
    public final StateFlow s;
    public final ConcurrentHashMap t;
    public final AtomicBoolean u;
    public Job v;
    public co w;
    public String x;
    public int y;
    public final MusicPlayerController$playerListener$1 z;

    /* JADX WARN: Type inference failed for: r7v3, types: [com.skiy.lane.app.playback.MusicPlayerController$playerListener$1] */
    public MusicPlayerController(Context context, CoroutineScope coroutineScope, UserRepository userRepository, TrackRepository trackRepository, PlaylistRepository playlistRepository, TrackStreamCacheManager trackStreamCacheManager, AppEvents appEvents, Cache cache) {
        coroutineScope.getClass();
        userRepository.getClass();
        trackRepository.getClass();
        playlistRepository.getClass();
        trackStreamCacheManager.getClass();
        appEvents.getClass();
        cache.getClass();
        this.a = coroutineScope;
        this.b = userRepository;
        this.c = trackRepository;
        this.d = playlistRepository;
        this.e = trackStreamCacheManager;
        this.f = appEvents;
        this.g = cache;
        MutableStateFlow mutableStateFlowA = StateFlowKt.a(new PlayerState(false, null, false, RepeatMode.a, false));
        this.j = mutableStateFlowA;
        this.k = FlowKt.c(mutableStateFlowA);
        MutableStateFlow mutableStateFlowA2 = StateFlowKt.a(EmptyList.a);
        this.l = mutableStateFlowA2;
        this.m = FlowKt.c(mutableStateFlowA2);
        MutableStateFlow mutableStateFlowA3 = StateFlowKt.a(null);
        this.n = mutableStateFlowA3;
        this.o = FlowKt.c(mutableStateFlowA3);
        MutableStateFlow mutableStateFlowA4 = StateFlowKt.a(null);
        this.p = mutableStateFlowA4;
        this.q = FlowKt.c(mutableStateFlowA4);
        MutableStateFlow mutableStateFlowA5 = StateFlowKt.a(new PlayerProgress(0, 0L, 0L));
        this.r = mutableStateFlowA5;
        this.s = FlowKt.c(mutableStateFlowA5);
        this.t = new ConcurrentHashMap();
        this.u = new AtomicBoolean(false);
        ExecutorService executorService = LaneLogger.a;
        ListenableFuture<MediaController> listenableFutureBuildAsync = new MediaController.Builder(context, new SessionToken(context, new ComponentName(context, (Class<?>) MusicService.class))).buildAsync();
        this.h = listenableFutureBuildAsync;
        if (listenableFutureBuildAsync != null) {
            listenableFutureBuildAsync.addListener(new Runnable() { // from class: com.skiy.lane.app.playback.d
                /* JADX WARN: Multi-variable type inference failed */
                @Override // java.lang.Runnable
                public final void run() {
                    MusicPlayerController musicPlayerController = this.a;
                    try {
                        ListenableFuture listenableFuture = musicPlayerController.h;
                        MediaController mediaController = listenableFuture != null ? (MediaController) listenableFuture.get() : null;
                        musicPlayerController.i = mediaController;
                        if (mediaController != null) {
                            mediaController.addListener(musicPlayerController.z);
                        }
                        ExecutorService executorService2 = LaneLogger.a;
                        musicPlayerController.f();
                        Job job = musicPlayerController.v;
                        if (job != null) {
                            ((JobSupport) job).cancel((CancellationException) null);
                        }
                        CoroutineScope coroutineScope2 = musicPlayerController.a;
                        DefaultScheduler defaultScheduler = Dispatchers.a;
                        musicPlayerController.v = BuildersKt.c(coroutineScope2, MainDispatcherLoader.a, new MusicPlayerController$startProgressUpdater$1(musicPlayerController, null), 2);
                        co coVar = musicPlayerController.w;
                        if (coVar != null) {
                            coVar.invoke();
                        }
                        musicPlayerController.w = null;
                    } catch (Exception unused) {
                        ExecutorService executorService3 = LaneLogger.a;
                    }
                }
            }, MoreExecutors.a());
        }
        BuildersKt.c(coroutineScope, null, new MusicPlayerController$listenForAppEvents$1(this, null), 3);
        this.z = new Player.Listener() { // from class: com.skiy.lane.app.playback.MusicPlayerController$playerListener$1
            @Override // androidx.media3.common.Player.Listener
            public final void onEvents(Player player, Player.Events events) {
                player.getClass();
                events.getClass();
                boolean zContains = events.contains(1);
                MusicPlayerController musicPlayerController = this.a;
                if (zContains) {
                    musicPlayerController.r.setValue(new PlayerProgress(0, 0L, 0L));
                    MediaItem currentMediaItem = player.getCurrentMediaItem();
                    if (!Intrinsics.c(currentMediaItem != null ? currentMediaItem.mediaId : null, musicPlayerController.x)) {
                        musicPlayerController.y = 0;
                        musicPlayerController.x = null;
                    }
                }
                musicPlayerController.f();
            }

            @Override // androidx.media3.common.Player.Listener
            public final void onPlaybackStateChanged(int i) {
                super.onPlaybackStateChanged(i);
                if (i == 4) {
                    this.a.b(false);
                }
            }

            @Override // androidx.media3.common.Player.Listener
            public final void onPlayerError(PlaybackException playbackException) {
                playbackException.getClass();
                if (playbackException.errorCode == 2008) {
                    MusicPlayerController musicPlayerController = this.a;
                    MediaController mediaController = musicPlayerController.i;
                    MediaItem currentMediaItem = mediaController != null ? mediaController.getCurrentMediaItem() : null;
                    String str = currentMediaItem != null ? currentMediaItem.mediaId : null;
                    if (mediaController != null && str != null) {
                        if (str.equals(musicPlayerController.x) && musicPlayerController.y >= 3) {
                            ExecutorService executorService2 = LaneLogger.a;
                            mediaController.seekToNext();
                            mediaController.prepare();
                            mediaController.play();
                            return;
                        }
                        musicPlayerController.x = str;
                        musicPlayerController.y++;
                        ExecutorService executorService3 = LaneLogger.a;
                        try {
                            musicPlayerController.g.k(str);
                        } catch (Exception unused) {
                            ExecutorService executorService4 = LaneLogger.a;
                        }
                        mediaController.seekTo(mediaController.getCurrentMediaItemIndex(), mediaController.getCurrentPosition());
                        mediaController.prepare();
                        mediaController.play();
                        return;
                    }
                }
                playbackException.getErrorCodeName();
                playbackException.getMessage();
                for (Throwable cause = playbackException.getCause(); cause != null; cause = cause.getCause()) {
                    cause.getMessage();
                }
                ExecutorService executorService5 = LaneLogger.a;
            }
        };
    }

    /* JADX WARN: Code duplicated, block: B:7:0x0013  */
    public static final Object a(MusicPlayerController musicPlayerController, Continuation continuation) {
        MusicPlayerController$refreshCurrentMediaItemConfig$1 musicPlayerController$refreshCurrentMediaItemConfig$1;
        MediaItem currentMediaItem;
        String string;
        MediaController mediaController;
        Uri uri;
        if (continuation instanceof MusicPlayerController$refreshCurrentMediaItemConfig$1) {
            musicPlayerController$refreshCurrentMediaItemConfig$1 = (MusicPlayerController$refreshCurrentMediaItemConfig$1) continuation;
            int i = musicPlayerController$refreshCurrentMediaItemConfig$1.d;
            if ((i & Integer.MIN_VALUE) != 0) {
                musicPlayerController$refreshCurrentMediaItemConfig$1.d = i - Integer.MIN_VALUE;
            } else {
                musicPlayerController$refreshCurrentMediaItemConfig$1 = new MusicPlayerController$refreshCurrentMediaItemConfig$1(musicPlayerController, continuation);
            }
        } else {
            musicPlayerController$refreshCurrentMediaItemConfig$1 = new MusicPlayerController$refreshCurrentMediaItemConfig$1(musicPlayerController, continuation);
        }
        Object obj = musicPlayerController$refreshCurrentMediaItemConfig$1.b;
        CoroutineSingletons coroutineSingletons = CoroutineSingletons.a;
        int i2 = musicPlayerController$refreshCurrentMediaItemConfig$1.d;
        Unit unit = Unit.a;
        if (i2 == 0) {
            ResultKt.b(obj);
            MediaController mediaController2 = musicPlayerController.i;
            if (mediaController2 != null && (currentMediaItem = mediaController2.getCurrentMediaItem()) != null) {
                String str = currentMediaItem.mediaId;
                str.getClass();
                TrackData trackData = (TrackData) musicPlayerController.t.get(str);
                if (trackData != null) {
                    MediaItem.LocalConfiguration localConfiguration = currentMediaItem.localConfiguration;
                    if (localConfiguration == null || (uri = localConfiguration.uri) == null || (string = uri.toString()) == null) {
                        string = MediaItem.DEFAULT_MEDIA_ID;
                    }
                    musicPlayerController$refreshCurrentMediaItemConfig$1.a = mediaController2;
                    musicPlayerController$refreshCurrentMediaItemConfig$1.d = 1;
                    MediaItem mediaItemG = g(trackData, string);
                    if (mediaItemG != coroutineSingletons) {
                        obj = mediaItemG;
                        mediaController = mediaController2;
                    }
                }
            }
        }
        if (i2 != 1) {
            if (i2 == 2) {
                ResultKt.b(obj);
                return unit;
            }
            i.l("call to 'resume' before 'invoke' with coroutine");
            return null;
        }
        mediaController = musicPlayerController$refreshCurrentMediaItemConfig$1.a;
        ResultKt.b(obj);
        DefaultScheduler defaultScheduler = Dispatchers.a;
        MainCoroutineDispatcher mainCoroutineDispatcher = MainDispatcherLoader.a;
        MusicPlayerController$refreshCurrentMediaItemConfig$2 musicPlayerController$refreshCurrentMediaItemConfig$2 = new MusicPlayerController$refreshCurrentMediaItemConfig$2(mediaController, (MediaItem) obj, null);
        musicPlayerController$refreshCurrentMediaItemConfig$1.a = null;
        musicPlayerController$refreshCurrentMediaItemConfig$1.d = 2;
        return BuildersKt.e(mainCoroutineDispatcher, musicPlayerController$refreshCurrentMediaItemConfig$2, musicPlayerController$refreshCurrentMediaItemConfig$1) == coroutineSingletons ? coroutineSingletons : unit;
    }

    public static MediaItem g(TrackData trackData, String str) {
        MediaMetadata mediaMetadataBuild = new MediaMetadata.Builder().setTitle(trackData.getTitle()).setArtist(trackData.getArtistsDisplayedName()).setArtworkUri(!StringsKt.t(trackData.getCoverUrl()) ? Uri.parse(trackData.getCoverUrl()) : null).build();
        mediaMetadataBuild.getClass();
        MediaItem.Builder mediaMetadata = new MediaItem.Builder().setMediaId(trackData.getSongId()).setUri(str).setCustomCacheKey(trackData.getSongId()).setMediaMetadata(mediaMetadataBuild);
        mediaMetadata.getClass();
        MediaItem mediaItemBuild = mediaMetadata.build();
        mediaItemBuild.getClass();
        return mediaItemBuild;
    }

    public final void b(boolean z) {
        if (((String) this.n.getValue()) == null) {
            return;
        }
        AtomicBoolean atomicBoolean = this.u;
        if (atomicBoolean.get()) {
            ExecutorService executorService = LaneLogger.a;
            return;
        }
        MediaController mediaController = this.i;
        if (mediaController != null && mediaController.getShuffleModeEnabled()) {
            ExecutorService executorService2 = LaneLogger.a;
            return;
        }
        List list = (List) this.l.getValue();
        if (list.isEmpty()) {
            return;
        }
        TrackData trackData = (TrackData) CollectionsKt.G(list);
        ExecutorService executorService3 = LaneLogger.a;
        trackData.getTitle();
        if (atomicBoolean.compareAndSet(false, true)) {
            MutableStateFlow mutableStateFlow = this.j;
            mutableStateFlow.setValue(PlayerState.a((PlayerState) mutableStateFlow.getValue(), true));
            BuildersKt.c(this.a, null, new MusicPlayerController$startWave$1(this, trackData, z, null), 3);
        }
    }

    public final void c(Function1 function1) {
        MediaController mediaController = this.i;
        if (mediaController != null) {
            function1.invoke(mediaController);
        } else {
            this.w = new co(this, function1, 5);
        }
    }

    public final void d(final String str, final int i, final List list) {
        list.getClass();
        str.getClass();
        if (list.isEmpty()) {
            ExecutorService executorService = LaneLogger.a;
        } else {
            c(new Function1() { // from class: com.skiy.lane.app.playback.a
                /* JADX WARN: Code duplicated, block: B:33:0x00a5  */
                /* JADX WARN: Code duplicated, block: B:36:0x00b2 A[LOOP:2: B:34:0x00ac->B:36:0x00b2, LOOP_END] */
                @Override // kotlin.jvm.functions.Function1
                public final Object invoke(Object obj) {
                    TrackData trackData;
                    MediaController mediaController = (MediaController) obj;
                    mediaController.getClass();
                    int i2 = i;
                    List<TrackData> list2 = list;
                    int i3 = (i2 < 0 || i2 >= list2.size()) ? 0 : i2;
                    if (i2 != i3) {
                        ExecutorService executorService2 = LaneLogger.a;
                        list2.size();
                    }
                    MusicPlayerController musicPlayerController = this;
                    MutableStateFlow mutableStateFlow = musicPlayerController.n;
                    ConcurrentHashMap concurrentHashMap = musicPlayerController.t;
                    Object value = mutableStateFlow.getValue();
                    String str2 = str;
                    boolean zC = Intrinsics.c(value, str2);
                    for (TrackData trackData2 : list2) {
                        concurrentHashMap.put(trackData2.getSongId(), trackData2);
                    }
                    if (!zC || (trackData = (TrackData) CollectionsKt.C(i3, list2)) == null) {
                        ExecutorService executorService3 = LaneLogger.a;
                        musicPlayerController.n.setValue(str2);
                        if (!zC) {
                            concurrentHashMap.clear();
                            for (TrackData trackData3 : list2) {
                                concurrentHashMap.put(trackData3.getSongId(), trackData3);
                            }
                        }
                        BuildersKt.c(musicPlayerController.a, null, new MusicPlayerController$play$1$3(list2, musicPlayerController, str2, mediaController, i3, null), 3);
                    } else {
                        Timeline currentTimeline = mediaController.getCurrentTimeline();
                        currentTimeline.getClass();
                        int windowCount = currentTimeline.getWindowCount();
                        for (int i4 = 0; i4 < windowCount; i4++) {
                            Timeline.Window window = new Timeline.Window();
                            currentTimeline.getWindow(i4, window);
                            if (Intrinsics.c(window.mediaItem.mediaId, trackData.getSongId())) {
                                ExecutorService executorService4 = LaneLogger.a;
                                trackData.getTitle();
                                mediaController.seekTo(i4, 0L);
                                if (mediaController.getPlaybackState() == 1 || mediaController.getPlaybackState() == 4) {
                                    mediaController.prepare();
                                }
                                if (!mediaController.isPlaying()) {
                                    mediaController.play();
                                }
                            }
                        }
                        ExecutorService executorService5 = LaneLogger.a;
                        ExecutorService executorService6 = LaneLogger.a;
                        musicPlayerController.n.setValue(str2);
                        if (!zC) {
                            concurrentHashMap.clear();
                            while (r12.hasNext()) {
                                concurrentHashMap.put(trackData3.getSongId(), trackData3);
                            }
                        }
                        BuildersKt.c(musicPlayerController.a, null, new MusicPlayerController$play$1$3(list2, musicPlayerController, str2, mediaController, i3, null), 3);
                    }
                    return Unit.a;
                }
            });
        }
    }

    public final void e(String str, String str2) {
        str.getClass();
        BuildersKt.c(this.a, null, new MusicPlayerController$playSingleTrackById$1(this, str, str2, null), 3);
    }

    public final void f() {
        String string;
        String string2;
        String string3;
        String str;
        MediaController mediaController = this.i;
        if (mediaController == null) {
            return;
        }
        MediaItem currentMediaItem = mediaController.getCurrentMediaItem();
        ConcurrentHashMap concurrentHashMap = this.t;
        TrackData trackData = (currentMediaItem == null || (str = currentMediaItem.mediaId) == null) ? null : (TrackData) concurrentHashMap.get(str);
        this.p.setValue(trackData);
        boolean zIsPlaying = mediaController.isPlaying();
        boolean shuffleModeEnabled = mediaController.getShuffleModeEnabled();
        int repeatMode = mediaController.getRepeatMode();
        this.j.setValue(new PlayerState(zIsPlaying, trackData, shuffleModeEnabled, repeatMode != 1 ? repeatMode != 2 ? RepeatMode.a : RepeatMode.b : RepeatMode.c, mediaController.getPlaybackState() == 2));
        Timeline currentTimeline = mediaController.getCurrentTimeline();
        currentTimeline.getClass();
        boolean zIsEmpty = currentTimeline.isEmpty();
        MutableStateFlow mutableStateFlow = this.l;
        if (zIsEmpty) {
            mutableStateFlow.setValue(EmptyList.a);
            return;
        }
        ArrayList arrayList = new ArrayList();
        boolean shuffleModeEnabled2 = mediaController.getShuffleModeEnabled();
        int firstWindowIndex = currentTimeline.getFirstWindowIndex(shuffleModeEnabled2);
        int windowCount = currentTimeline.getWindowCount();
        for (int i = 0; firstWindowIndex != -1 && i < windowCount; i++) {
            Timeline.Window window = new Timeline.Window();
            currentTimeline.getWindow(firstWindowIndex, window);
            String str2 = window.mediaItem.mediaId;
            str2.getClass();
            TrackData trackData2 = (TrackData) concurrentHashMap.get(str2);
            if (trackData2 != null) {
                arrayList.add(trackData2);
            } else {
                MediaMetadata mediaMetadata = window.mediaItem.mediaMetadata;
                mediaMetadata.getClass();
                CharSequence charSequence = mediaMetadata.title;
                if (charSequence == null || (string = charSequence.toString()) == null) {
                    string = "Unknown";
                }
                String str3 = string;
                CharSequence charSequence2 = mediaMetadata.artist;
                String str4 = (charSequence2 == null || (string3 = charSequence2.toString()) == null) ? MediaItem.DEFAULT_MEDIA_ID : string3;
                Uri uri = mediaMetadata.artworkUri;
                arrayList.add(new TrackData(str2, (String) null, str3, str4, (uri == null || (string2 = uri.toString()) == null) ? MediaItem.DEFAULT_MEDIA_ID : string2, (String) null, (String) null, (SpotifyAdditionalData) null, (SoundCloudAdditionalData) null, (DeezerAdditionalData) null, (TelegramAdditionalData) null, (List) null, 4066, (DefaultConstructorMarker) null));
            }
            firstWindowIndex = currentTimeline.getNextWindowIndex(firstWindowIndex, 0, shuffleModeEnabled2);
        }
        mutableStateFlow.setValue(arrayList);
    }

    public final void h() {
        c(new iq(21));
    }
}
