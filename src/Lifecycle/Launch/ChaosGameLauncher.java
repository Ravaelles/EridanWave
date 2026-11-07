package Lifecycle.Launch;

import java.io.File;

/**
 * Original Windows backend: live StarCraft + ChaosLauncher.
 * Kills leftovers, patches {@code bwapi.ini}, then starts ChaosLauncher
 * (which in turn autostarts StarCraft).
 */
public final class ChaosGameLauncher implements GameLauncher {

    @Override
    public void launch(LaunchConfig config) {
        System.out.println("[launcher] Backend: Windows + StarCraft + ChaosLauncher.");

        Lifecycle.Keyboard.Keyboard.listen(Lifecycle.Keyboard.Keyboard.Mode.BOT);

        ProcessHelper.killStarcraft();
        ProcessHelper.killChaosLauncher();

        File ini = new File(ProcessHelper.defaultFile(config.getBwapiIni(), "bwapi-data/bwapi.ini"));
        BwapiIni.patch(ini, config.getMap(), config.getRace(), config.getEnemyRace(),
            config.getGameType(), false);

        // IMPORTANT: ChaosLauncher -> Settings -> "Run Starcraft on Startup" must be checked.
        String chaosLauncher = ProcessHelper.defaultFile(config.getChaosLauncherExe(),
            "chaoslauncher/Chaoslauncher.exe");
        System.out.println("[launcher] Starting ChaosLauncher: " + chaosLauncher);
        ProcessHelper.startChaosLauncherWindows(chaosLauncher);
    }
}
