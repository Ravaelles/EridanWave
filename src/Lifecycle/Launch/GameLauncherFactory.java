package Lifecycle.Launch;

/**
 * Single decision point for the game backend. No backend {@code if}-s are
 * scattered across game logic. Defaults to today's behaviour (attach straight
 * to the running game); {@code --launcher=} switches backends:
 * <ul>
 *   <li>{@code none} (default) - attach straight to the running game (ladder, tournaments);</li>
 *   <li>{@code chaos} - Windows, native StarCraft + ChaosLauncher;</li>
 *   <li>{@code wine} - Linux, real StarCraft + ChaosLauncher under Wine.</li>
 * </ul>
 *
 * <p>The Wine environment wins over the flag: the bot client JVM under Wine is
 * started bare (no CLI args carry over from {@code wine-run.sh}), so the flag
 * alone can never route it - but the Wine markers in its environment can.
 * Without this the client falls through to the no-op backend and its global
 * hook (and key relay drain) never starts.</p>
 */
public final class GameLauncherFactory {

    private GameLauncherFactory() {
    }

    public static GameLauncher forConfig(LaunchConfig config) {
        if (isWineClientJvm() || runningUnderWine()) {
            return new WineClientGameLauncher();
        }
        switch (config.getLauncher()) {
            case WINE:
                // Reached only by a Linux JVM (a Wine JVM returns early
                // above): the IDE/supervisor, routed to scripts/wine-run.sh.
                // A Linux JVM must never attach directly - JBWAPI would pick
                // the POSIX connection backend and loop on "Game table mapping
                // not found" forever, never seeing the Wine-side shared memory.
                return new UnixChaosGameLauncher();
            case CHAOS:
                return new ChaosGameLauncher();
            case NONE:
            default:
                return new NoOpGameLauncher();
        }
    }

    /**
     * True when this JVM was started as the Wine game client by
     * {@code scripts/wine-run.sh} (explicit {@code -Deridanwave.wineClient},
     * immune to Wine stripping its own env vars before the Windows process
     * sees them). The client gets no CLI args, so the flag alone can never
     * route it - this marker is the routing signal.
     */
    public static boolean isWineClientJvm() {
        String marker = System.getProperty("eridanwave.wineClient");
        if (marker == null) marker = System.getProperty("purplewave.wineClient");

        return "true".equals(marker);
    }

    /**
     * True inside a one-shot scbw Docker match container: scbw names every
     * game GAME_* and tears the container down afterwards, so the bot JVM
     * must exit when the match ends instead of idling for the next game
     * (which never comes - scbw reports the linger as a crash after 70 s).
     * Ladder/tournament harnesses never set GAME_NAME, so their process
     * reuse across games is unaffected.
     */
    public static boolean isScbwMatchContainer() {
        String gameName = System.getenv("GAME_NAME");

        return gameName != null && gameName.startsWith("GAME_");
    }

    /**
     * True when this JVM itself is a Windows process running under Wine -
     * detected via the env markers Wine sets for its child processes.
     */
    static boolean runningUnderWine() {
        return System.getenv("WINEDEBUG") != null
            || System.getenv("WINEDLLOVERRIDES") != null
            || System.getenv("WINEPREFIX") != null;
    }
}
