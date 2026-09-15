package com.github.username000101;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;

import java.io.File;
import java.io.IOException;

public class Emulator {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    private final DvmClass BNITManager;

    public Emulator(String processName, String className, String libraryForLoading, String[] dependencies) {
        emulator = AndroidEmulatorBuilder.for64Bit()
                .setProcessName(processName)
                .addBackendFactory(new Unicorn2Factory(true))
                .build();
        final var memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        vm = emulator.createDalvikVM();
        vm.setVerbose(false);
        for (var dependency : dependencies)
            vm.loadLibrary(new File(dependency), false);
        System.out.println();

        var dm = vm.loadLibrary(new File(libraryForLoading), false);
        dm.callJNI_OnLoad(emulator);
        module = dm.getModule();

        BNITManager = vm.resolveClass(className);
    }

    @FunctionalInterface
    public interface  ExtendedIOHandler {
        FileResult<AndroidFileIO> handleIO(String pathname, int flags, com.github.unidbg.Emulator<AndroidFileIO> emulator) throws IOException;
    }

    public void newUniversalIOHandler(ExtendedIOHandler handler) {
        emulator.getSyscallHandler().addIOResolver((emulator, pathname, oflags) ->
        {
            try {
                return handler.handleIO(pathname, oflags, emulator);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Object callCdrl(String method, String path, String query, byte[] body, long timechototam) {
        return BNITManager.callStaticJniMethodObject(
                emulator,
                "cdrl(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[BJ)[[B",
                method, path, query, body, timechototam
        );
    }
}
