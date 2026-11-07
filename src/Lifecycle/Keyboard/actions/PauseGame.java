package Lifecycle.Keyboard.actions;

import Lifecycle.Keyboard.GameCommands;
import Lifecycle.Keyboard.KeyAction;

/** Keys Space/`/Pause : toggle pause. Reads the real game state, so chat-command pauses stay in sync. */
public final class PauseGame implements KeyAction {

    @Override
    public void execute() {
        GameCommands.togglePause();
    }
}
