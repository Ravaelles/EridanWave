package Lifecycle.Launch;

import java.io.File;

/**
 * Kills and starts processes: StarCraft and ChaosLauncher natively on Windows,
 * the same pair under Wine on Linux.
 */
public final class ProcessHelper {

    private ProcessHelper() {
    }

    public static boolean isWindows() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase(java.util.Locale.ROOT).contains("win");
    }

    public static void killStarcraft() {
        if (isWindows()) {
            execQuiet("cmd", "/c", "taskkill /IM StarCraft.exe /T /F");
        } else {
            execQuiet("pkill", "-9", "-x", "StarCraft.exe");
        }
    }

    public static void killChaosLauncher() {
        if (isWindows()) {
            execQuiet("cmd", "/c", "taskkill /IM Chaoslauncher.exe /T /F");
        } else {
            // Never -x here: the 17-char name exceeds the 15-char comm limit,
            // so -x never matches; the bracket keeps pgrep from matching this
            // shell's own command line.
            execQuiet("pkill", "-9", "-f", "[C]haoslauncher.exe");
        }
    }

    /** The bot's own JVM under Wine (named java.exe, distinct from Linux java). */
    public static void killClientJvm() {
        if (isWindows()) {
            execQuiet("taskkill", "/IM", "java.exe", "/T", "/F");
        } else {
            execQuiet("pkill", "-9", "-x", "java.exe");
        }
    }

    public static boolean isProcessRunning(String exactName) {
        try {
            Process check = isWindows()
                ? new ProcessBuilder("cmd", "/c", "tasklist /FI \"IMAGENAME eq " + exactName + "\"").start()
                : new ProcessBuilder("pgrep", "-x", exactName).start();
            int exit = check.waitFor();
            if (isWindows()) {
                byte[] out = readAll(check);
                return new String(out).contains(exactName);
            }
            return exit == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** Autostart ChaosLauncher on Windows (needs "Run Starcraft on Startup" checked). */
    public static void startChaosLauncherWindows(String chaosLauncherExe) {
        try {
            Thread.sleep(150);
        } catch (InterruptedException ignored) {
        }
        execQuiet("cmd", "/c", chaosLauncherExe);
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    private static void execQuiet(String... command) {
        try {
            new ProcessBuilder(command).redirectErrorStream(true).start().waitFor();
        } catch (Exception ignored) {
        }
    }

    private static byte[] readAll(Process process) {
        try {
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = process.getInputStream().read(chunk)) >= 0) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    static String defaultFile(String override, String fallback) {
        if (override != null && !override.trim().isEmpty()) {
            return override.trim();
        }
        return fallback;
    }

    static String userHome() {
        String home = System.getProperty("user.home", "");
        if (home.endsWith("/") || home.endsWith(File.separator)) {
            return home;
        }
        return home + File.separator;
    }
}
