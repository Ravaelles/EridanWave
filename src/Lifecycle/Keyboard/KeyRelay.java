package Lifecycle.Keyboard;

import Lifecycle.Launch.LaunchPaths;

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
 *
 * <p>Both sides must resolve the SAME file, but they run with different
 * working directories (supervisor: repo root, client: game root) and in
 * different filename namespaces (Linux paths vs Wine/Windows paths, where
 * drive mappings like {@code Z:} are not guaranteed to exist). The rendezvous
 * is therefore the live game root, derivable on both sides without any path
 * translation: the script passes the absolute Windows path down
 * ({@code -Deridanwave.keyRelay}, e.g. {@code C:\sc\...}, mirroring
 * {@code eridanwave.bwapiIni}), while the supervisor derives the Unix side
 * from the Wine prefix layout. The historical CWD-relative file stays as the
 * last resort for hand-started runs.</p>
 */
public final class KeyRelay {

    private KeyRelay() {
    }

    static File file() {
        String override = LaunchPaths.property("keyRelay");
        if (override == null) {
            override = LaunchPaths.env("KEYRELAY");
        }

        File resolved = LaunchPaths.keyRelayFile(
            override,
            gameRootDir(),
            System.getProperty("user.dir", "."));

        return resolved;
    }

    /**
     * Unix side of the live game root. Custom roots reach the supervisor
     * through {@code ERIDANWAVE_GAME_ROOT} (legacy {@code PURPLEWAVE_GAME_ROOT}
     * also read); the client always gets its absolute Windows path via
     * {@code -Deridanwave.keyRelay}, so it never needs this. The layout
     * itself lives in {@link LaunchPaths#gameRoot}, shared with the
     * bwapi.ini path - only the env chain stays here, since the relay
     * file must resolve with no CLI flags on either side.
     */
    static String gameRootDir() {
        return gameRootDir(LaunchPaths.env("GAME_ROOT"), System.getenv("WINEPREFIX"));
    }

    static String gameRootDir(String gameRoot, String winePrefix) {
        if (gameRoot != null && !gameRoot.trim().isEmpty()) {
            return gameRoot.trim();
        }

        return LaunchPaths.gameRoot(winePrefix, null);
    }

    /** Absolute path of the relay file, for startup diagnostics on both sides. */
    public static String describe() {
        try {
            return file().getAbsolutePath();
        } catch (SecurityException e) {
            return "(unresolvable)";
        }
    }
    /** Supervisor side: record a key code for the bot to execute. Never throws. */
    public static void send(int keyCode, int keyLocation) {
        try {
            File file = file();
            file.getParentFile().mkdirs();
            Files.write(
                file.toPath(),
                (keyCode + ":" + keyLocation + ":" + System.currentTimeMillis() + "\n").getBytes(StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
        }
    }

    /**
     * Bot side: take every key queued so far and remove the file.
     * Each entry is {@code [code, location, supervisorMillis]}; lines in the
     * old stamp-less shape yield millis {@code 0} (always executed - with the
     * startup cleanup no stale lines survive a session boundary anyway).
     */
    public static List<long[]> drain() {
        List<long[]> codes = new ArrayList<>();
        File file = file();
        if (!file.isFile()) return codes;

        try {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                try {
                    String[] parts = line.trim().split(":", 3);
                    if (parts.length == 3) {
                        codes.add(new long[]{Long.parseLong(parts[0]), Long.parseLong(parts[1]), Long.parseLong(parts[2])});
                    } else if (parts.length == 2) {
                        codes.add(new long[]{Long.parseLong(parts[0]), Long.parseLong(parts[1]), 0});
                    } else {
                        codes.add(new long[]{Long.parseLong(parts[0]), 1, 0});
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


