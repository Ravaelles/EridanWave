package Lifecycle.Keyboard.actions;

import Lifecycle.Keyboard.GameCommands;
import Lifecycle.Keyboard.KeyAction;

/** Base for keys "1"-"9": jump straight to a rung of the game-speed ladder. */
public abstract class SetGameSpeed implements KeyAction {

    private final int speed;
    private final int frameSkip;

    protected SetGameSpeed(int speed, int frameSkip) {
        this.speed = speed;
        this.frameSkip = frameSkip;
    }

    @Override
    public void execute() {
        GameCommands.setSpeed(speed, frameSkip);
    }
}
