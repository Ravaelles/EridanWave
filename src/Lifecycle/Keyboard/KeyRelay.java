package Lifecycle.Keyboard;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * The key channel between the two JVMs of the Wine setup.
 *
 * <p>The bot's JVM runs under Wine, so its global hook only sees keys typed
 * into Wine windows, while the Linux supervisor JVM sees every key but has no
 * game. So the supervisor captures keys and writes their codes to a file;
 * the bot drains the file every frame and executes them.</p>
 *
 * <p>The file is a plain append log, drained by deleting: the supervisor
 * appends key codes, the bot reads everything and removes the file.</p>
 */
public final class KeyRelay {

    private KeyRelay() {
    }

    private static File file() {
        File direct = new File("out/wine/keys.txt");
        if (direct.getParentFile().isDirectory()) {
            return direct;
        }
        File fallback = new File(System.getProperty("user.dir", ".") + "/out/wine/keys.txt");
        fallback.getParentFile().mkdirs();
        return fallback;
    }

    /** Supervisor side: record a key code for the bot to execute. Never throws. */
    public static void send(int keyCode, int keyLocation) {
        try {
            File file = file();
            file.getParentFile().mkdirs();
            Files.write(
                file.toPath(),
                (keyCode + ":" + keyLocation + "\n").getBytes(StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
        }
    }

    /** Bot side: take every key queued so far (code:location lines) and remove the file. */
    public static List<int[]> drain() {
        List<int[]> codes = new ArrayList<>();
        File file = file();
        if (!file.isFile()) return codes;
        try {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                try {
                    String[] parts = line.trim().split(":", 2);
                    if (parts.length == 2) {
                        codes.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
                    } else {
                        codes.add(new int[]{Integer.parseInt(parts[0]), 1});
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            Files.delete(file.toPath());
        } catch (IOException ignored) {
        }
        return codes;
    }
}
