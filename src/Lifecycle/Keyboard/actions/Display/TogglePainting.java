package Lifecycle.Keyboard.actions.Display;

import Lifecycle.Keyboard.GameCommands;
import Lifecycle.Keyboard.KeyAction;

/** Key "p" : toggle debug painting. */
public final class TogglePainting implements KeyAction {

    @Override
    public void execute() {
        GameCommands.togglePainting();
    }
}
