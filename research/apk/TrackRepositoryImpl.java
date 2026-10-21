package com.skiy.lane.data.repository;

import com.skiy.lane.data.remote.api.TracksApi;
import com.skiy.lane.domain.repository.TrackRepository;
import kotlin.Metadata;
import kotlinx.coroutines.flow.Flow;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes.dex */
@Metadata(d1 = {"\u0000\u000e\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u00012\u00020\u0002¨\u0006\u0003"}, d2 = {"Lcom/skiy/lane/data/repository/TrackRepositoryImpl;", "Lcom/skiy/lane/data/repository/BaseRepository;", "Lcom/skiy/lane/domain/repository/TrackRepository;", "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class TrackRepositoryImpl extends BaseRepository implements TrackRepository {
    public final TracksApi a;

    public TrackRepositoryImpl(TracksApi tracksApi) {
        tracksApi.getClass();
        this.a = tracksApi;
    }

    @Override // com.skiy.lane.domain.repository.TrackRepository
    public final Flow A0(String str) {
        str.getClass();
        return BaseRepository.E0(new TrackRepositoryImpl$search$1(this, str, null));
    }

    @Override // com.skiy.lane.domain.repository.TrackRepository
    public final Flow K(String str, String str2) {
        str.getClass();
        str2.getClass();
        return BaseRepository.E0(new TrackRepositoryImpl$getWaveByTrack$1(this, str, str2, null));
    }

    @Override // com.skiy.lane.domain.repository.TrackRepository
    public final Flow S(String str) {
        str.getClass();
        return BaseRepository.E0(new TrackRepositoryImpl$getTrackStats$1(this, str, null));
    }

    @Override // com.skiy.lane.domain.repository.TrackRepository
    public final Flow j(String str, String str2) {
        str2.getClass();
        return BaseRepository.E0(new TrackRepositoryImpl$getTrackEffect$1(this, str, str2, null));
    }

    @Override // com.skiy.lane.domain.repository.TrackRepository
    public final Flow k(String str) {
        str.getClass();
        return BaseRepository.E0(new TrackRepositoryImpl$getLyrics$1(this, str, null));
    }

    @Override // com.skiy.lane.domain.repository.TrackRepository
    public final Flow k0(String str) {
        return BaseRepository.E0(new TrackRepositoryImpl$downloadTrack$1(this, str, null));
    }

    @Override // com.skiy.lane.domain.repository.TrackRepository
    public final Flow o(String str, String str2, String str3) {
        str.getClass();
        return BaseRepository.E0(new TrackRepositoryImpl$getGenericStream$1(this, str, str2, str3, null));
    }

    @Override // com.skiy.lane.domain.repository.TrackRepository
    public final Flow p0(String str, String str2) {
        str.getClass();
        return BaseRepository.E0(new TrackRepositoryImpl$createItemShareLink$1(this, str, str2, null));
    }

    @Override // com.skiy.lane.domain.repository.TrackRepository
    public final Flow v0(String str) {
        return BaseRepository.E0(new TrackRepositoryImpl$logTrackPlay$1(this, str, null));
    }
}
