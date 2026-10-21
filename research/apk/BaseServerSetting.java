package com.skiy.lane.domain;

import androidx.media3.common.MediaItem;
import kotlin.Metadata;
import kotlin.text.StringsKt;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes.dex */
@Metadata(d1 = {"\u0000\n\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\bÇ\u0002\u0018\u00002\u00020\u0001¨\u0006\u0002"}, d2 = {"Lcom/skiy/lane/domain/BaseServerSetting;", MediaItem.DEFAULT_MEDIA_ID, "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class BaseServerSetting {
    public static volatile String a = "https://laneapi.com";
    public static volatile String b = "https://cdn.laneapi.com";

    public static void a(String str, String str2) {
        if (str != null && !StringsKt.t(str)) {
            a = str;
        }
        if (str2 == null || StringsKt.t(str2)) {
            return;
        }
        b = str2;
    }
}
