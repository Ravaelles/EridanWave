package Lifecycle.Keyboard.actions.Display;

import Lifecycle.Keyboard.GameCommands;
import Lifecycle.Keyboard.KeyAction;

/** Key "v" : toggle the visualization overlay. */
public final class ToggleVisualization implements KeyAction {

    @Override
    public void execute() {
        GameCommands.toggleVisualization();
    }
}
