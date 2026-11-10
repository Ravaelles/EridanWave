package Lifecycle.Launch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Linux backend: real StarCraft 1.16.1 + BWAPI 4.4.0 + ChaosLauncher under
 * Wine. This JVM is the IDE's Linux JVM - it can never BE the bot (JBWAPI
 * picks the POSIX connection backend and never sees the Wine-side BWAPI
 * shared memory), so it acts as the supervisor: it runs
 * {@code scripts/wine-run.sh} with inherited I/O so the owner sees
 * everything in the IDE console, and exits with the script's exit code.
 * The real bot is the Windows-JRE JVM the script starts under Wine.
 *
 * <p>When this JVM itself already runs under Wine (Windows process), the game
 * host is already up and this process is a client that only attaches: patch
 * bwapi.ini and return, without starting anything or killing anything.</p>
 */
public final class UnixChaosGameLauncher implements GameLauncher {

    @Override
    public void launch(LaunchConfig config) {
        if (GameLauncherFactory.runningUnderWine()) {
            System.out.println("===============================================");
            System.out.println("[launcher] Wine client (JVM under Wine, attaching to the game).");
            System.out.println("===============================================");

            if ( ! GameLauncherFactory.isHeadlessRun()) {
                Lifecycle.Keyboard.Keyboard.listen(Lifecycle.Keyboard.Keyboard.Mode.BOT);
            }

            File ini = defaultWineIni(config);
            BwapiIni.patch(ini, config.getMap(), config.getRace(), config.getEnemyRace(),
                config.getGameType(), true);
            // Do NOT start ChaosLauncher, do NOT pkill: the game is already up.

            return;
        }

        System.out.println("===============================================");
        System.out.println("[launcher] Backend: Wine + StarCraft + ChaosLauncher (Linux).");
        System.out.println("===============================================");

        // Script default is headless; the hook lives only on explicit --visible.
        if (config.isHeadless() || ! config.isHeadlessExplicit()) {
            System.out.println("[keyboard] Headless run: supervisor shortcuts disabled.");
        } else {
            Lifecycle.Keyboard.Keyboard.listen(Lifecycle.Keyboard.Keyboard.Mode.SUPERVISOR);
        }

        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add(findScript());
        if (config.getMap() != null) {
            command.add("--map=" + config.getMap());
        }
        if (config.getRace() != null) {
            command.add("--race=" + config.getRace());
        }
        if (config.getEnemyRace() != null) {
            command.add("--enemy-race=" + config.getEnemyRace());
        }
        if (config.getGameType() != null) {
            command.add("--game-type=" + config.getGameType());
        }
        if (config.getSpeed() != null) {
            command.add("--speed=" + config.getSpeed());
        }
        if (config.isCherryVisExplicit()) {
            command.add("--cherryvis=" + config.isCherryVis());
        }
        if (config.getJar() != null) {
            command.add("--jar=" + config.getJar());
        }
        if (config.getWinePrefix() != null) {
            command.add("--wine-prefix=" + config.getWinePrefix());
        }
        if (config.getGameRoot() != null) {
            command.add("--game-root=" + config.getGameRoot());
        }
        if (config.getJavaExe() != null) {
            command.add("--java-exe=" + config.getJavaExe());
        }
        if (config.getTuningStrategy() != null) {
            command.add("--tuning-strategy=" + config.getTuningStrategy());
        }
        for (java.util.Map.Entry<String, String> tuneParam : config.getTuneParams().entrySet()) {
            command.add("--tune-param=" + tuneParam.getKey() + "=" + tuneParam.getValue());
        }
        if (config.isHeadlessExplicit()) {
            command.add(config.isHeadless() ? "--headless" : "--visible");
        }

        ProcessBuilder supervisor = new ProcessBuilder(command);
        supervisor.inheritIO();
        try {
            int exitCode = supervisor.start().waitFor();
            System.out.println("[launcher] wine-run.sh finished with exit code " + exitCode);
            System.exit(exitCode);
        } catch (Exception e) {
            System.err.println("[launcher] Failed to run the Wine supervisor: " + e.getMessage());
            System.err.println("[launcher] Run it by hand: bash scripts/wine-run.sh --help");
            System.exit(1);
        }
    }

    private static String findScript() {
        if (new File("scripts/wine-run.sh").isFile()) {
            return "scripts/wine-run.sh";
        }

        throw new IllegalStateException(
            "scripts/wine-run.sh not found; run the bot from the repo root.");
    }

    static File defaultWineIni(LaunchConfig config) {
        if (config.getBwapiIni() != null) {
            return new File(config.getBwapiIni());
        }

        return BwapiIni.defaultLiveIni(config.getWinePrefix(), config.getGameRoot());
    }
}


