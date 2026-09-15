// lane-reverse
// attempt to port Lane
//
// usage:
// <executable> <bearer token> <libraries directory> <overlay directory>

package com.github.username000101;

import com.github.unidbg.file.FileResult;
import com.github.unidbg.linux.file.ByteArrayFileIO;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 3)
            HelpCommand.printHelp();

        final var libsDir = args[0];

        final var method = args[1];
        final var path = args[2];

        final var libBnit = Paths.get(libsDir, "libbnit.so");
        final var libCpp = Paths.get(libsDir, "libc++_shared.so");
        final var libCrypto = Paths.get(libsDir, "libcrypto.so");
        final var libSsl = Paths.get(libsDir, "libssl.so");

        if (!Files.exists(libSsl))
            throw new FileNotFoundException(libSsl.toString());
        if (!Files.exists(libCrypto))
            throw new FileNotFoundException(libCrypto.toString());
        if (!Files.exists(libCpp))
            throw new FileNotFoundException(libCpp.toString());
        if (!Files.exists(libBnit))
            throw new FileNotFoundException(libBnit.toString());

        Emulator emulator = new Emulator(
                "com.skiy.lane",
                "com/skiy/lane/app/BNITManager",
                libBnit.toString(),
                new String[] {
                        libSsl.toString(),
                        libCrypto.toString(),
                        libCpp.toString(),
                }
        );

        emulator.newUniversalIOHandler(((pathname, flags, emulatorObj) -> {
            String fixedPath = "";
            if (pathname.startsWith("/")) fixedPath = "." + pathname;
            else if (pathname.startsWith("./")) fixedPath = pathname;
            else fixedPath = "./" + pathname;

            final var overlayPath = Paths.get(fixedPath);

            if (Files.exists(overlayPath)) {
                var content = Files.readString(overlayPath);
                return FileResult.success(new ByteArrayFileIO(flags, pathname, content.getBytes()));
            }
            return null;
        }));

        JLog.printHeaders(emulator, method, path);
    }
}
