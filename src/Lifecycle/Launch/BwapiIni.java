package Lifecycle.Launch;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Rewrites the live {@code bwapi.ini} before the game starts: our race, the
 * enemy race, the map and the game type. Only lines that actually change are
 * written back, so untouched runs leave the file alone.
 *
 * <p>Both spellings are matched ({@code key=value} and {@code key = value});
 * missing keys are appended. The map family decides the default game type
 * (sscai/... maps play MELEE, everything else USE_MAP_SETTINGS); an explicit
 * {@code --game-type=} always wins.</p>
 */
public final class BwapiIni {

    /** Live bwapi.ini as the game reads it (Wine prefix layout). */
    public static File defaultLiveIni() {
        return defaultLiveIni(null, null);
    }

    /**
     * Explicit path wins (the Wine supervisor passes it as
     * {@code -Deridanwave.bwapiIni=...} because user.home guesses are
     * unreliable under Wine; the legacy {@code -Dpurplewave.bwapiIni=...}
     * is still accepted).
     */
    private static String explicitIniPath() {
        String path = System.getProperty("eridanwave.bwapiIni");
        if (path == null || path.trim().isEmpty()) {
            path = System.getProperty("purplewave.bwapiIni");
        }
        if (path != null && !path.trim().isEmpty()) {
            return path.trim();
        }

        return null;
    }

    /** Live game root with explicit Wine layout overrides (null = default). */
    public static String defaultGameRoot(String winePrefix, String gameRoot) {
        if (gameRoot != null && !gameRoot.trim().isEmpty()) {
            return gameRoot.trim();
        }

        if (winePrefix == null || winePrefix.trim().isEmpty()) {
            winePrefix = defaultWinePrefix();
        }
        return winePrefix + "/drive_c/sc";
    }

    /** Live bwapi.ini with explicit Wine layout overrides (null = default). */
    public static File defaultLiveIni(String winePrefix, String gameRoot) {
        String explicit = explicitIniPath();
        if (explicit != null) {
            return new File(explicit);
        }

        return new File(defaultGameRoot(winePrefix, gameRoot) + "/bwapi-data/bwapi.ini");
    }

    private static String defaultWinePrefix() {
        String prefix = System.getenv("WINEPREFIX");
        if (prefix != null && !prefix.trim().isEmpty()) {
            return prefix.trim();
        }

        return ProcessHelper.userHome() + ".wine";
    }

    private BwapiIni() {
    }

    /**
     * Default game type for a map: MELEE unless the path shows scenario
     * content. Ladder packs (sscai/, BroodWar/) are melee; anything under a
     * ums/ folder or with a micro-map name fragment plays USE_MAP_SETTINGS.
     * Keep in sync with the marker list in scripts/wine-run.sh.
     * An explicit --game-type= always wins over this default.
     */
    private static final String[] UMS_MARKERS = {
        "ums", "micro", "minimap", "defense", "defence", "scenario", "bullet",
        "_v_", "vsgosu", "exp_skilltest", "dragoon_v", "zeal_v", "goon_v",
        "wraith_v", "lings_v", "marines_v", "jim_v_", "mar_v_", "rebas",
        "hydraling", "m&m", "sunken", "reav_v", "probe_run", "drag_run",
        "zealdrag", "wounded", "incorner", "tanks_v", "gosu", "7th"
    };

    /** True for maps that play USE_MAP_SETTINGS (scenario content). */
    public static boolean isUmsMap(String resolvedMapPath) {
        if (resolvedMapPath == null) {
            return false;
        }

        String lower = resolvedMapPath.toLowerCase(java.util.Locale.ROOT);
        for (String marker : UMS_MARKERS) {
            if (lower.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    public static String defaultGameTypeForMap(String resolvedMapPath) {
        return isUmsMap(resolvedMapPath) ? "USE_MAP_SETTINGS" : "MELEE";
    }

    /**
     * @param ini            live bwapi.ini (Wine-side on the wine backend).
     * @param map            exact map path as the game must load it
     *                       (e.g. {@code maps/sscai/(3)TauCross.scx}), or null to keep.
     * @param race           our race, or null to keep.
     * @param enemyRace      enemy race, or null to keep.
     * @param gameType       explicit game type, or null for the map-family default.
     * @param enforceWindowlessOnWine true to force {@code windowed = OFF} (the Wine
     *                       virtual desktop fills the window; the game's own
     *                       windowed mode would nest a second small window).
     * @return true when the file was rewritten.
     */
    public static boolean patch(File ini, String map, String race, String enemyRace,
            String gameType, boolean enforceWindowlessOnWine) {
        if (ini == null || !ini.isFile()) {
            System.out.println("[launcher] bwapi.ini not found at " + ini + "; leaving it unchanged");

            return false;
        }

        List<String> lines;
        try {
            lines = new ArrayList<>(Files.readAllLines(ini.toPath(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.out.println("[launcher] Could not read " + ini + ": " + e.getMessage());

            return false;
        }

        boolean changed = false;
        String effectiveGameType = gameType;
        if (map != null && effectiveGameType == null) {
            effectiveGameType = defaultGameTypeForMap(map);
        }
        changed |= setKey(lines, "map", map);
        changed |= setKey(lines, "race", race);
        changed |= setKey(lines, "enemy_race", enemyRace);
        changed |= setKey(lines, "game_type", effectiveGameType);
        if (enforceWindowlessOnWine) {
            changed |= setKey(lines, "windowed", "OFF");
        }

        if (!changed) {
            return false;
        }

        try {
            StringBuilder out = new StringBuilder();
            for (String line : lines) {
                out.append(line).append('\n');
            }
            Files.write(ini.toPath(), out.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.out.println("[launcher] Could not write " + ini + ": " + e.getMessage());

            return false;
        }
        System.out.println("[launcher] Patched " + ini);

        return true;
    }

    private static boolean setKey(List<String> lines, String key, String value) {
        if (value == null) {
            return false;
        }

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.startsWith(key + "=") || line.startsWith(key + " =")) {
                String replacement = key + "=" + value;
                if (!lines.get(i).equals(replacement)) {
                    lines.set(i, replacement);
                    System.out.println("[launcher]   " + key + " -> " + value);

                    return true;
                }

                return false;
            }
        }
        lines.add(key + "=" + value);
        System.out.println("[launcher]   " + key + " -> " + value + " (appended)");

        return true;
    }
}


