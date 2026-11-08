package Lifecycle.Launch;

/**
 * Command line configuration for launching a game around the bot.
 *
 * <p>Supported arguments (all optional; bare {@code java -jar bot.jar} keeps
 * today's behaviour of attaching straight to the game):</p>
 * <ul>
 *   <li>{@code --launcher=none|chaos|wine} (default {@code none}).
 *       {@code chaos} = native Windows + ChaosLauncher,
 *       {@code wine} = real StarCraft under Wine on Linux.</li>
 *   <li>{@code --map=&lt;name&gt;} map file name, e.g. {@code (3)TauCross.scx}
 *       or a path relative to the game's {@code maps/} folder.</li>
 *   <li>{@code --race=Protoss|Terran|Zerg|Random} our race (written to bwapi.ini).</li>
 *   <li>{@code --enemy-race=...} enemy race (written to bwapi.ini).</li>
 *   <li>{@code --game-type=MELEE|USE_MAP_SETTINGS} overrides the map-family default.</li>
  *   <li>{@code --speed=&lt;0-1000&gt;} default game speed (20 if absent; keyboard presets still work).</li>
 *   <li>{@code --cherryvis[=&lt;bool&gt;]} write the CherryVis viewer trace
 *       (default {@code true}). Accepts {@code 1/true/yes} and
 *       {@code 0/false/no} (case-insensitive); {@code --no-cherryvis}
 *       disables it.</li>
 *   <li>{@code --headless} run the Wine session on Xvfb :99 (nothing on :0).</li>
 *   <li>{@code --visible} run on the current X display instead.
 *       Explicit visibility is forwarded to {@code wine-run.sh}; without an
 *       explicit flag the script default (headless) applies.</li>
 *   <li>{@code --jar=&lt;path&gt;} bot jar for the Wine client (default
 *       {@code target/EridanWave-1.0.jar} under the repo root).</li>
 *   <li>{@code --wine-prefix=&lt;path&gt;} (default {@code ~/.wine}).</li>
 *   <li>{@code --game-root=&lt;path&gt;} StarCraft install (default
 *       {@code $WINEPREFIX/drive_c/sc}).</li>
 *   <li>{@code --java-exe=&lt;path&gt;} Windows JRE under Wine (default
 *       {@code $WINEPREFIX/drive_c/Java/bin/java.exe}).</li>
 *   <li>{@code --bwapi-ini=&lt;path&gt;} live bwapi.ini (default
 *       {@code $GAME_ROOT/bwapi-data/bwapi.ini}).</li>
 *   <li>{@code --chaoslauncher-exe=&lt;path&gt;} ChaosLauncher (Windows backend).</li>
 *   <li>{@code --tuning-strategy=&lt;name&gt;} tuning strategy from
 *       {@code Tuning.Strategies} (or a fully qualified class name), e.g.
 *       {@code KiteAwayDuel}. Absent = default play, no tuning.</li>
 *   <li>{@code --tune-param NAME=VALUE} (repeatable) per-game param override;
 *       absent keys randomize within bounds. Example:
 *       {@code --tune-param KiteAway.seriousCooldown=8}.</li>
 *   <li>{@code --help} print this and exit.</li>
 * </ul>
 */
public final class LaunchConfig {

    public enum Launcher {
        NONE,
        CHAOS,
        WINE
    }

    private Launcher launcher = Launcher.NONE;
    private String map;
    private String race;
    private String enemyRace;
    private String gameType;
    private Integer speed;
    private boolean headless;
    private boolean headlessExplicit;
    private Boolean cherryvis;
    private String jar;
    private String winePrefix;
    private String gameRoot;
    private String javaExe;
    private String bwapiIni;
    private String chaosLauncherExe;
    private boolean helpRequested;
    private String tuningStrategy;
    private final java.util.Map<String, String> tuneParams = new java.util.LinkedHashMap<>();

    private LaunchConfig() {
    }

    public static LaunchConfig parse(String[] args) {
        LaunchConfig config = new LaunchConfig();
        if (args != null) {
            for (String arg : args) {
                if (arg == null) continue;
                if (arg.equals("--help") || arg.equals("-h")) {
                    config.helpRequested = true;
                } else if (arg.startsWith("--launcher=")) {
                    String value = arg.substring("--launcher=".length()).trim().toUpperCase();
                    try {
                        config.launcher = Launcher.valueOf(value);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Unknown --launcher=" + value + " (none|chaos|wine)");
                    }
                } else if (arg.startsWith("--map=")) {
                    config.map = arg.substring("--map=".length()).trim();
                } else if (arg.startsWith("--enemy-race=")) {
                    config.enemyRace = normalizeRace(arg.substring("--enemy-race=".length()));
                } else if (arg.startsWith("--race=")) {
                    config.race = normalizeRace(arg.substring("--race=".length()));
                } else if (arg.startsWith("--game-type=")) {
                    config.gameType = arg.substring("--game-type=".length()).trim().toUpperCase();
                } else if (arg.startsWith("--speed=")) {
                    config.speed = parseSpeed(arg.substring("--speed=".length()));
                } else if (arg.equals("--cherryvis")) {
                    config.cherryvis = true;
                } else if (arg.equals("--no-cherryvis")) {
                    config.cherryvis = false;
                } else if (arg.startsWith("--cherryvis=")) {
                    config.cherryvis = parseFlagBoolean(arg.substring("--cherryvis=".length()), "--cherryvis");
                } else if (arg.equals("--headless")) {
                    config.headless = true;
                    config.headlessExplicit = true;
                } else if (arg.startsWith("--headless=")) {
                    config.headless = parseFlagBoolean(arg.substring("--headless=".length()), "--headless");
                    config.headlessExplicit = true;
                } else if (arg.equals("--visible")) {
                    config.headless = false;
                    config.headlessExplicit = true;
                } else if (arg.startsWith("--jar=")) {
                    config.jar = arg.substring("--jar=".length()).trim();
                } else if (arg.startsWith("--wine-prefix=")) {
                    config.winePrefix = arg.substring("--wine-prefix=".length()).trim();
                } else if (arg.startsWith("--game-root=")) {
                    config.gameRoot = arg.substring("--game-root=".length()).trim();
                } else if (arg.startsWith("--java-exe=")) {
                    config.javaExe = arg.substring("--java-exe=".length()).trim();
                } else if (arg.startsWith("--bwapi-ini=")) {
                    config.bwapiIni = arg.substring("--bwapi-ini=".length()).trim();
                } else if (arg.startsWith("--chaoslauncher-exe=")) {
                    config.chaosLauncherExe = arg.substring("--chaoslauncher-exe=".length()).trim();
                } else if (arg.startsWith("--tuning-strategy=")) {
                    config.tuningStrategy = arg.substring("--tuning-strategy=".length()).trim();
                } else if (arg.startsWith("--tune-param=")) {
                    String kv = arg.substring("--tune-param=".length());
                    int eq = kv.indexOf('=');
                    if (eq < 0) throw new IllegalArgumentException("Want --tune-param NAME=VALUE, got: " + arg);
                    config.tuneParams.put(kv.substring(0, eq).trim(), kv.substring(eq + 1).trim());
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg + " (try --help)");
                }
            }
        }
        return config;
    }

    private static String normalizeRace(String value) {
        String race = value.trim();
        if (race.equalsIgnoreCase("random")) return "Random";
        if (race.equalsIgnoreCase("protoss")) return "Protoss";
        if (race.equalsIgnoreCase("terran")) return "Terran";
        if (race.equalsIgnoreCase("zerg")) return "Zerg";
        if (race.equalsIgnoreCase("P") || race.equalsIgnoreCase("T")
                || race.equalsIgnoreCase("Z") || race.equalsIgnoreCase("R")) {
            return race.toUpperCase();
        }
        throw new IllegalArgumentException("Unknown race: " + value + " (Protoss|Terran|Zerg|Random)");
    }

    /**
     * Lenient true/false parsing shared by all boolean flags.
     * Accepts {@code 1/true/yes} and {@code 0/false/no}, case-insensitive.
     */
    public static boolean parseFlagBoolean(String value, String flag) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        if (normalized.equals("1") || normalized.equals("true") || normalized.equals("yes")) {
            return true;
        }
        if (normalized.equals("0") || normalized.equals("false") || normalized.equals("no")) {
            return false;
        }
        throw new IllegalArgumentException(
            "Not a boolean: " + flag + "=" + value + " (1/true/yes or 0/false/no)");
    }

    private static Integer parseSpeed(String value) {
        try {
            int speed = Integer.parseInt(value.trim());
            if (speed < 0 || speed > 1000) {
                throw new IllegalArgumentException("Out of range --speed=" + value + " (0-1000)");
            }
            return speed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a number: --speed=" + value + " (0-1000)");
        }
    }

    public static void printHelp() {
        System.out.println("EridanWave game launcher");
        System.out.println("  java -jar EridanWave.jar [--launcher=none|chaos|wine] [--map=<name>]");
        System.out.println("      [--race=Protoss|Terran|Zerg|Random] [--enemy-race=...]");
        System.out.println("      [--game-type=MELEE|USE_MAP_SETTINGS] [--speed=0-1000] [--headless] [--jar=<path>]");
        System.out.println("      [--cherryvis[=1|true|yes|0|false|no]] [--no-cherryvis]");
        System.out.println("      [--wine-prefix=<path>] [--game-root=<path>] [--java-exe=<path>]");
        System.out.println("      [--bwapi-ini=<path>] [--chaoslauncher-exe=<path>]");
        System.out.println("      [--tuning-strategy=<name>] [--tune-param NAME=VALUE]...");
        System.out.println("No arguments = today's behaviour: attach straight to the running game.");
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public boolean isHelpRequested() {
        return helpRequested;
    }

    public String getMap() {
        return map;
    }

    public String getRace() {
        return race;
    }

    public String getEnemyRace() {
        return enemyRace;
    }

    public String getGameType() {
        return gameType;
    }

    public Integer getSpeed() {
        return speed;
    }

    public boolean isHeadless() {
        return headless;
    }

    /**
     * True when --headless/--headless=/--visible was passed explicitly.
     * Without it the supervisor forwards nothing and the script default
     * (headless) applies, so an explicit --visible actually arrives.
     */
    public boolean isHeadlessExplicit() {
        return headlessExplicit;
    }

    /**
     * Effective value: true unless explicitly disabled. Null means the flag
     * was never passed, so the config file / environment still decide.
     */
    public boolean isCherryVis() {
        return cherryvis == null || cherryvis;
    }

    public boolean isCherryVisExplicit() {
        return cherryvis != null;
    }

    public String getJar() {
        return jar;
    }

    public String getWinePrefix() {
        return winePrefix;
    }

    public String getGameRoot() {
        return gameRoot;
    }

    public String getJavaExe() {
        return javaExe;
    }

    public String getBwapiIni() {
        return bwapiIni;
    }

    public String getChaosLauncherExe() {
        return chaosLauncherExe;
    }

    public String getTuningStrategy() {
        return tuningStrategy;
    }

    public java.util.Map<String, String> getTuneParams() {
        return tuneParams;
    }
}
