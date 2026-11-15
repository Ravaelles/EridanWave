package Lifecycle.Launch;

import java.io.File;

/**
 * Where the live game lives (one home for the Wine path rendezvous).
 *
 * <p>The bot runs in up to three filename namespaces at once (Linux paths,
 * Wine/Windows paths, and the legacy {@code purplewave.*} spellings), and
 * until now {@code BwapiIni} and {@code KeyRelay} each reimplemented the
 * fallback chains by hand - and already disagree (no {@code PURPLEWAVE_GAME_ROOT}
 * on one side, no {@code -Deridanwave.gameRoot} on the other, {@code ~/.wine}
 * spelled two ways). Everything path-shaped funnels through here; callers
 * pass explicit overrides and take a {@code File} back, which is also what
 * makes them testable with a temp dir instead of the process environment.</p>
 */
public final class LaunchPaths {

    private LaunchPaths() {
    }

    /**
     * System property {@code eridanwave.name}, legacy {@code purplewave.name}
     * fallback. Trimmed, null when blank or unset.
     */
    public static String property(String name) {
        String value = System.getProperty("eridanwave." + name);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty("purplewave." + name);
        }

        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    /**
     * Environment {@code ERIDANWAVE_NAME}, legacy {@code PURPLEWAVE_NAME}
     * fallback. Trimmed, null when blank or unset.
     */
    public static String env(String name) {
        String value = System.getenv("ERIDANWAVE_" + name);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv("PURPLEWAVE_" + name);
        }

        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    /** Explicit override or fallback (both trimmed). */
    static String defaultFile(String override, String fallback) {
        if (override != null && !override.trim().isEmpty()) {
            return override.trim();
        }

        return fallback;
    }

    /** user.home with a trailing separator, never blank. */
    static String userHome() {
        String home = System.getProperty("user.home", "");
        if (home.endsWith("/") || home.endsWith(File.separator)) {
            return home;
        }

        return home + File.separator;
    }

    /**
     * BwapiIni's wine prefix: {@code WINEPREFIX} only (no legacy spelling -
     * that variable was never read here), else the user home.
     */
    public static String defaultWinePrefix() {
        return defaultWinePrefix(System.getenv("WINEPREFIX"));
    }

    /** Same, with the environment value passed in so suites can pin it. */
    public static String defaultWinePrefix(String winePrefixEnv) {
        if (winePrefixEnv != null && !winePrefixEnv.trim().isEmpty()) {
            return winePrefixEnv.trim();
        }

        return userHome() + ".wine";
    }

    /** Live game root: explicit layout wins, else the Wine prefix layout. */
    public static String gameRoot(String winePrefix, String gameRoot) {
        if (gameRoot != null && !gameRoot.trim().isEmpty()) {
            return gameRoot.trim();
        }

        if (winePrefix == null || winePrefix.trim().isEmpty()) {
            winePrefix = defaultWinePrefix();
        }

        return winePrefix + "/drive_c/sc";
    }

    /** Live bwapi.ini: explicit path wins, else the game root layout. */
    public static File bwapiIni(String explicitPath, String winePrefix, String gameRoot) {
        if (explicitPath != null && !explicitPath.trim().isEmpty()) {
            return new File(explicitPath.trim());
        }

        return new File(gameRoot(winePrefix, gameRoot) + "/bwapi-data/bwapi.ini");
    }

    /**
     * Key relay file: explicit path wins, else the shared game-root file
     * when its directory exists, else the CWD-relative file, else a
     * user-dir fallback (created). All inputs explicit, so suites drive it
     * with a temp dir instead of the process environment.
     */
    public static File keyRelayFile(String relayOverride, String gameRoot, String userDir) {
        if (relayOverride != null && !relayOverride.trim().isEmpty()) {
            return new File(relayOverride.trim());
        }

        File shared = new File(gameRoot + "/bwapi-data/write/keys.txt");
        if (shared.getParentFile().isDirectory()) {
            return shared;
        }

        File direct = new File("out/wine/keys.txt");
        if (direct.getParentFile().isDirectory()) {
            return direct;
        }

        File fallback = new File(userDir + "/out/wine/keys.txt");
        fallback.getParentFile().mkdirs();

        return fallback;
    }
}
