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
 * (MELEE unless the path shows scenario content - see MapCatalog.umsMarkers;
 * an explicit {@code --game-type=} always wins).</p>
 */
public final class BwapiIni {

    /** Live bwapi.ini as the game reads it (Wine prefix layout). */
    public static File defaultLiveIni() {
        return defaultLiveIni(null, null);
    }

    /** Live game root with explicit Wine layout overrides (null = default). */
    public static String defaultGameRoot(String winePrefix, String gameRoot) {
        return LaunchPaths.gameRoot(winePrefix, gameRoot);
    }

    /** Live bwapi.ini with explicit Wine layout overrides (null = default). */
    public static File defaultLiveIni(String winePrefix, String gameRoot) {
        return LaunchPaths.bwapiIni(LaunchPaths.property("bwapiIni"), winePrefix, gameRoot);
    }

    private BwapiIni() {
    }

    /**
     * Default game type for a map: MELEE unless the path shows scenario
     * content (MapCatalog.umsMarkers). An explicit --game-type= always
     * wins over this default.
     */
    public static boolean isUmsMap(String resolvedMapPath) {
        return Lifecycle.MapCatalog.isUmsMap(resolvedMapPath);
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


