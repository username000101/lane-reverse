package com.github.username000101;

import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import net.dongliu.apk.parser.utils.Pair;

import java.nio.charset.Charset;
import java.util.ArrayList;

public class JLog {
    private static void createJSONResponse(ArrayList<Pair<String, String>> headers) {
        var xClientMeta = "";
        var xCoreToken = "";
        var xRequestTraceId = "";

        for (var header : headers) {
            if (header.getLeft().compareToIgnoreCase("x-core-token") == 0)
                xCoreToken = header.getRight();
            else if (header.getLeft().compareToIgnoreCase("x-client-meta") == 0)
                xClientMeta = header.getRight();
            else if (header.getLeft().compareToIgnoreCase("x-request-trace-id") == 0)
                xRequestTraceId = header.getRight();
        }

        System.out.printf(
                """
                {
                    "x-client-meta": "%s",
                    "x-request-trace-id": "%s",
                    "x-core-token": "%s"
                }
                """,
                xClientMeta, xRequestTraceId, xCoreToken);
    }

    public static void printHeaders(Emulator emulator, String method, String path) {
        final byte[] stubBytes = {};
        var extraHeaders = (ArrayObject)emulator.callCdrl(method, path, "", stubBytes, 0L);

        DvmObject<?>[] elements = extraHeaders.getValue();
        ArrayList<Pair<String, String>> result = new ArrayList<>();
        for (DvmObject<?> el : elements) {
            if (el instanceof ByteArray) {
                byte[] data = ((ByteArray) el).getValue();
                var headerStr = new String(data, Charset.defaultCharset());

                // idk why but array contains empty string
                if (headerStr.isEmpty()) continue;

                var headerParts = headerStr.split(":");
                result.add(new Pair<>(headerParts[0], headerParts[1]));
            }
        }
        createJSONResponse(result);
    }
}
