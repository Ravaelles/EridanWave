package Lifecycle.Keyboard.actions.Display;

import Lifecycle.Keyboard.GameCommands;
import Lifecycle.Keyboard.KeyAction;

/** Key "c" : toggle camera-follow mode (same switch as the chat "c" command). */
public final class ToggleCamera implements KeyAction {

    @Override
    public void execute() {
        GameCommands.toggleCamera();
    }
}
