package Lifecycle.Launch;

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

    /**
     * Shuts down the Wine server for this prefix, taking the whole Wine
     * session (StarCraft, ChaosLauncher and the client JVM) down at once.
     * One call replaces the three name-based kills above: no process-name
     * matching, so nothing outside Wine can be hit (docker games live in
     * their own PID namespaces with their own wineservers and are invisible
     * to the host). The Linux environment (including WINEPREFIX) is
     * inherited, so this targets the same prefix the game runs in. Xvfb and
     * the Linux supervisor JVM survive: neither is a Wine process.
     */
    public static void killWineServer() {
        execQuiet("wineserver", "-k");
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
}


