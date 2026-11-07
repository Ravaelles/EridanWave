package Lifecycle.Keyboard.actions;

import Lifecycle.Keyboard.GameCommands;
import Lifecycle.Keyboard.KeyAction;

/** Key "-"/"[" : one step slower on the game-speed ladder. */
public final class SlowerGame implements KeyAction {

    @Override
    public void execute() {
        GameCommands.slower();
    }
}
