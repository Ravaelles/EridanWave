package Lifecycle.Launch;

/**
 * Default backend: do nothing. The bot attaches straight to the running game,
 * exactly like a bare {@code java -jar PurpleWave.jar} always did (ladder,
 * tournaments, ChaosLauncher started by hand).
 */
public final class NoOpGameLauncher implements GameLauncher {

    @Override
    public void launch(LaunchConfig config) {
        // Nothing to prepare; PurpleBWClient.startGame connects next.
    }
}
