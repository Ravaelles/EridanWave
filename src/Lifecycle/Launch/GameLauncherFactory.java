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
 */
public final class GameLauncherFactory {

    private GameLauncherFactory() {
    }

    public static GameLauncher forConfig(LaunchConfig config) {
        switch (config.getLauncher()) {
            case WINE:
                // WINECLIENT means "this JVM is the bot" - true only when the JVM
                // itself runs under Wine (a Windows process). A Linux JVM (the IDE)
                // selecting WINECLIENT would loop on "Game table mapping not found"
                // forever, because JBWAPI picks the POSIX connection backend and
                // never sees the Wine-side shared memory. So the IDE is routed to
                // the supervisor launcher instead, which runs scripts/wine-run.sh.
                return runningUnderWine()
                    ? new WineClientGameLauncher()
                    : new UnixChaosGameLauncher();
            case CHAOS:
                return new ChaosGameLauncher();
            case NONE:
            default:
                return new NoOpGameLauncher();
        }
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
