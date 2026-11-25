package Lifecycle.Keyboard.actions;

import Lifecycle.Keyboard.KeyAction;
import Lifecycle.Keyboard.Keyboard;
import Lifecycle.Launch.ProcessHelper;

/**
 * Escape: kill the game. On the supervisor that means taking StarCraft and
 * ChaosLauncher down (the wine-run.sh trap then tears the session down
 * and the supervisor JVM exits with it); on the bot/Windows JVM it ends this
 * process, which the supervisor observes the same way.
 */
public final class ExitGame implements KeyAction {

    @Override
    public void execute() {
        Keyboard.beginExit();
        System.out.println("[keyboard] Exit was requested manually.");
        if (Keyboard.isSupervisor()) {
            // One wineserver owns the whole Wine session (StarCraft,
            // ChaosLauncher and the client JVM), so shutting it down takes
            // the entire game down at once: no name matching, nothing
            // outside Wine can be hit.
            ProcessHelper.killWineServer();

            // Historical approach (kept for reference): three name-based
            // kills in a fixed order. The client went first, while still
            // reading, because killing StarCraft first dropped the BWAPI
            // server mid-frame and the client died with an EOF stack trace
            // instead of exiting.
            // ProcessHelper.killClientJvm();
            // ProcessHelper.sleep(500);
            // ProcessHelper.killStarcraft();
            // ProcessHelper.killChaosLauncher();

            return;
        }

        ProcessHelper.killStarcraft();
        ProcessHelper.killChaosLauncher();
        System.exit(0);
    }
}


