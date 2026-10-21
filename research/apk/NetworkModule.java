package com.skiy.lane.app.di;

import androidx.media3.common.MediaItem;
import dagger.Module;
import defpackage.zt;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import kotlin.Metadata;
import kotlin.collections.ArraysKt;
import kotlinx.serialization.json.Json;
import kotlinx.serialization.json.JsonKt;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.dnsoverhttps.DnsOverHttps;

/* JADX INFO: compiled from: r8-map-id-800769a2decd49bcddc10b2c7df9722533f146ac709ba5bb759fcce7e995ccbb */
/* JADX INFO: loaded from: classes.dex */
@Metadata(d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\bÇ\u0002\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003¨\u0006\u0004"}, d2 = {"Lcom/skiy/lane/app/di/NetworkModule;", MediaItem.DEFAULT_MEDIA_ID, "<init>", "()V", "app_release"}, k = 1, mv = {2, 2, 0}, xi = 48)
@Module
public final class NetworkModule {
    public static final NetworkModule a = new NetworkModule();
    public static final Json b = JsonKt.a(new zt(20));
    public static final ExecutorService c;

    static {
        ExecutorService executorServiceNewCachedThreadPool = Executors.newCachedThreadPool();
        executorServiceNewCachedThreadPool.getClass();
        c = executorServiceNewCachedThreadPool;
    }

    private NetworkModule() {
    }

    public static final DnsOverHttps a(OkHttpClient okHttpClient, String str, String... strArr) {
        DnsOverHttps.Builder builder = new DnsOverHttps.Builder();
        builder.a = okHttpClient;
        HttpUrl.INSTANCE.getClass();
        builder.b = HttpUrl.Companion.b(str);
        builder.c = false;
        ArrayList arrayList = new ArrayList(strArr.length);
        for (String str2 : strArr) {
            arrayList.add(InetAddress.getByName(str2));
        }
        InetAddress[] inetAddressArr = (InetAddress[]) arrayList.toArray(new InetAddress[0]);
        builder.e = ArraysKt.R((InetAddress[]) Arrays.copyOf(inetAddressArr, inetAddressArr.length));
        return builder.a();
    }
}
