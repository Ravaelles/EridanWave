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
            // Supervisor: kill the client JVM first, while it is still reading.
            // Killing StarCraft first would drop the BWAPI server mid-frame and
            // the client would die with an EOF stack trace instead of exiting.
            ProcessHelper.killClientJvm();
            ProcessHelper.sleep(500);
            ProcessHelper.killStarcraft();
            ProcessHelper.killChaosLauncher();

            return;
        }

        ProcessHelper.killStarcraft();
        ProcessHelper.killChaosLauncher();
        System.exit(0);
    }
}


