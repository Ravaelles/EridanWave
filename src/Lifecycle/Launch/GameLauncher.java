package Lifecycle.Launch;

/**
 * Strategy for preparing the game environment before the bot attaches.
 * Game logic never branches on the backend; each implementation owns one.
 */
public interface GameLauncher {

    /**
     * Prepares everything the chosen backend needs (map selection, bwapi.ini,
     * processes) and returns. The bot itself is started afterwards by the
     * caller via {@code PurpleBWClient.startGame(...)}.
     */
    void launch(LaunchConfig config);
}
