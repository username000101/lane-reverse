package com.skiy.lane.domain.model;

import androidx.media3.common.MediaItem;
import kotlin.Metadata;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;

/* JADX WARN: Enum visitor error
jadx.core.utils.exceptions.JadxRuntimeException: Can't remove SSA var: r0v1 com.skiy.lane.domain.model.AudioQuality[], still in use, count: 1, list:
  (r0v1 com.skiy.lane.domain.model.AudioQuality[]) from 0x0030: INVOKE (r0v1 com.skiy.lane.domain.model.AudioQuality[]) STATIC call: kotlin.enums.EnumEntriesKt.a(java.lang.Enum[]):kotlin.enums.EnumEntries A[MD:(java.lang.Enum[]):kotlin.enums.EnumEntries (m), WRAPPED] (LINE:49)
	at jadx.core.utils.InsnRemover.removeSsaVar(InsnRemover.java:164)
	at jadx.core.utils.InsnRemover.unbindResult(InsnRemover.java:129)
	at jadx.core.utils.InsnRemover.lambda$unbindInsns$1(InsnRemover.java:101)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
	at jadx.core.utils.InsnRemover.unbindInsns(InsnRemover.java:100)
	at jadx.core.utils.InsnRemover.removeAllAndUnbind(InsnRemover.java:257)
	at jadx.core.dex.visitors.EnumVisitor.convertToEnum(EnumVisitor.java:187)
	at jadx.core.dex.visitors.EnumVisitor.visit(EnumVisitor.java:102)
 */
/* JADX WARN: Failed to restore enum class, 'enum' modifier and super class removed */
/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes3.dex */
@Metadata(d1 = {"\u0000\n\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001¨\u0006\u0002"}, d2 = {"Lcom/skiy/lane/domain/model/AudioQuality;", MediaItem.DEFAULT_MEDIA_ID, "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class AudioQuality {
    c("BASIC", "128 kbps"),
    d("HIGH", "192 kbps"),
    e("ULTRA", "320 kbps");

    public static final /* synthetic */ EnumEntries l;
    public final String a;
    public final int b;

    static {
        l = EnumEntriesKt.a(audioQualityArr);
    }

    public AudioQuality(String str, String str2) {
        super(str, i);
        this.a = str2;
        this.b = i;
    }

    public static AudioQuality valueOf(String str) {
        return (AudioQuality) Enum.valueOf(AudioQuality.class, str);
    }

    public static AudioQuality[] values() {
        return (AudioQuality[]) f.clone();
    }
}
