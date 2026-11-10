package Lifecycle.Launch;

/**
 * Wine-JVM backend: the bot's own JVM runs <b>under Wine</b>, next to
 * StarCraft, so it can see BWAPI's Windows shared memory. Unlike
 * {@link UnixChaosGameLauncher} this launcher deliberately does <b>not</b>
 * start or kill anything: the game is brought up by
 * {@code scripts/wine-run.sh} (or a previous Linux-side bot start), the
 * JVM here is already a Windows process, and pkill/wmctrl/wine do not exist
 * for it. It only patches bwapi.ini and waits for
 * {@code EridanBWClient.startGame()} to attach.
 *
 * <p>Selected automatically: {@code --launcher=wine} on a Linux JVM routes to
 * the supervisor; a JVM that itself runs under Wine routes here.</p>
 */
public final class WineClientGameLauncher implements GameLauncher {

    @Override
    public void launch(LaunchConfig config) {
        System.out.println("===============================================");
        System.out.println("[launcher] Backend: Java under Wine (client only).");
        System.out.println("[launcher] Not starting the game - it must already");
        System.out.println("[launcher] run under Wine. Waiting for the BWAPI game table...");
        System.out.println("===============================================");

        if (GameLauncherFactory.isHeadlessRun()) {
            System.out.println("[keyboard] Headless run: global shortcuts disabled.");
        } else {
            Lifecycle.Keyboard.Keyboard.listen(Lifecycle.Keyboard.Keyboard.Mode.BOT);
        }

        BwapiIni.patch(UnixChaosGameLauncher.defaultWineIni(config),
            config.getMap(), config.getRace(), config.getEnemyRace(),
            config.getGameType(), true);
    }
}
