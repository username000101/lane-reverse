//package com.github.username000101;
//
//import com.github.unidbg.linux.android.dvm.DvmObject;
//import com.github.unidbg.linux.android.dvm.array.ArrayObject;
//import com.github.unidbg.linux.android.dvm.array.ByteArray;
//import net.dongliu.apk.parser.utils.Pair;
//
//import java.io.IOException;
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.nio.charset.Charset;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.zip.GZIPInputStream;
//
//public class HttpTestService {
//    private final String BASE_URL = "https://ru.laneapi.com";
//
//    private final Pair<String, String>[] TYPICAL_HEADERS;
//
//    private final Emulator emulator;
//    private final HttpClient client;
//
//    public HttpTestService(Emulator emulator) {
//        this.emulator = emulator;
//        this.client = HttpClient.newHttpClient();
//        this.TYPICAL_HEADERS = new Pair[] {
//                new Pair<>("authorization", "Bearer " + BuildConfig.BEARER_TOKEN),
//                //new Pair<>("x-app-version", ""),
//
//                // idk why but if add this header
//                // responses will be encoded (??)
//                new Pair<>("x-accept-red", BuildConfig.ACCEPT_RED ? "1" : "0"),
//                new Pair<>("accept-language", BuildConfig.ACCEPT_LANGUAGE),
//                new Pair<>("ldi", BuildConfig.LDI),
//                new Pair<>("tz", BuildConfig.TIMEZONE),
//
//                // in request dumps this header duplicates
//                // idk why
//                new Pair<>("x-app-version", "207"),
//                new Pair<>("x-platform", "android"),
//                new Pair<>("x-theme", "light"),
//                new Pair<>("user-agent", "LaneMusic/1.0 (Android; Mobile)"),
//                new Pair<>("accept-encoding", "application/json")
//        };
//    }
//}
