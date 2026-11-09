package Lifecycle.Keyboard.actions.Display;

import Lifecycle.Keyboard.GameCommands;
import Lifecycle.Keyboard.KeyAction;

/** Key "0" : toggle the game GUI (considerably faster headless play). */
public final class ToggleGui implements KeyAction {

    @Override
    public void execute() {
        GameCommands.toggleGui();
    }
}
