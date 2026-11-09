package Lifecycle.Keyboard.actions.Speed;

import Lifecycle.Keyboard.GameCommands;
import Lifecycle.Keyboard.KeyAction;

/** Key "+"/"]" : one step faster on the game-speed ladder. */
public final class FasterGame implements KeyAction {

    @Override
    public void execute() {
        GameCommands.faster();
    }
}
