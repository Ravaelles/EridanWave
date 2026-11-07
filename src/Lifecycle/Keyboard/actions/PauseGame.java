package Lifecycle.Keyboard.actions;

import Lifecycle.Keyboard.GameCommands;
import Lifecycle.Keyboard.KeyAction;

/** Keys Space/`/Pause : toggle the Atlantis-style bot pause (the game keeps running). */
public final class PauseGame implements KeyAction {

    @Override
    public void execute() {
        GameCommands.togglePause();
    }
}
