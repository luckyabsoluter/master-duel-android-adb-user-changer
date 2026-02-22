package opensource.master_duel_android_adb_user_changer;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

final class JavafxWindowsNativeDllBootstrap {
    private static final String RESOURCE_DIR = "javafx-native/win";
    private static final String[] DLL_NAMES = {
            "glass.dll",
            "javafx_font.dll",
            "javafx_iio.dll",
            "prism_common.dll",
            "prism_sw.dll",
            "prism_d3d.dll",
            "decora_sse.dll"
    };
    private static final AtomicBoolean LOADED = new AtomicBoolean(false);

    private JavafxWindowsNativeDllBootstrap() {
    }

    static void preloadIfWindows() {
        if (!isWindows()) {
            return;
        }
        if (!LOADED.compareAndSet(false, true)) {
            return;
        }
        try {
            Path targetDir = Files.createTempDirectory("md-aauc-javafx-");
            targetDir.toFile().deleteOnExit();
            for (String dllName : DLL_NAMES) {
                extractDllTo(targetDir, dllName);
            }
            prependJavaLibraryPath(targetDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to prepare bundled JavaFX Windows native libraries.", e);
        }
    }

    private static void extractDllTo(Path targetDir, String dllName) throws IOException {
        String resourcePath = RESOURCE_DIR + "/" + dllName;
        try (InputStream input = JavafxWindowsNativeDllBootstrap.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Missing bundled JavaFX DLL resource: " + resourcePath);
            }
            Path target = targetDir.resolve(dllName);
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            target.toFile().deleteOnExit();
        }
    }

    private static void prependJavaLibraryPath(Path path) {
        String current = System.getProperty("java.library.path", "");
        String prefix = path.toAbsolutePath().toString();
        if (current.isBlank()) {
            System.setProperty("java.library.path", prefix);
            return;
        }
        if (current.startsWith(prefix + File.pathSeparator) || current.equals(prefix)) {
            return;
        }
        System.setProperty("java.library.path", prefix + File.pathSeparator + current);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("windows");
    }
}
