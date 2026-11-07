package Lifecycle.Keyboard;

import Lifecycle.Keyboard.actions.ExitGame;
import Lifecycle.Keyboard.actions.FasterGame;
import Lifecycle.Keyboard.actions.PauseGame;
import Lifecycle.Keyboard.actions.SlowerGame;
import Lifecycle.Keyboard.actions.Speed1;
import Lifecycle.Keyboard.actions.Speed2;
import Lifecycle.Keyboard.actions.Speed3;
import Lifecycle.Keyboard.actions.Speed4;
import Lifecycle.Keyboard.actions.Speed5;
import Lifecycle.Keyboard.actions.Speed6;
import Lifecycle.Keyboard.actions.Speed7;
import Lifecycle.Keyboard.actions.Speed8;
import Lifecycle.Keyboard.actions.Speed9;
import Lifecycle.Keyboard.actions.ToggleCamera;
import Lifecycle.Keyboard.actions.ToggleGui;
import Lifecycle.Keyboard.actions.TogglePainting;
import Lifecycle.Keyboard.actions.ToggleVisualization;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global keyboard shortcuts for local development games. One class per button:
 * each key maps to exactly one {@link KeyAction}; this class only wires keys
 * to actions and routes them to the JVM that owns the game.
 *
 * <p>Two JVMs see different keys: the Linux supervisor hook sees every global
 * keystroke but has no game, so it forwards everything (except Escape, which
 * kills the game - exactly the supervisor's job) to the bot through
 * {@link KeyRelay}. The bot JVM under Wine executes directly (its hook only
 * sees Wine-window keys) and additionally drains the relay file every frame.
 * The Windows ChaosLauncher setup is a single JVM and executes directly.</p>
 *
 * <p>Never active in ladder/tournament mode ({@code --launcher=none}):
 * Escape must never kill a tournament game.</p>
 */
public final class Keyboard implements NativeKeyListener {

    /** Which JVM this hook lives in. */
    public enum Mode {
        /** Linux supervisor: no game here, forward to the bot (Escape kills). */
        SUPERVISOR,
        /** The bot itself (Wine JVM or Windows): execute directly. */
        BOT
    }

    private static volatile boolean active;
    private static volatile Mode mode = Mode.BOT;
    private static volatile boolean exiting;

    private static final Map<Integer, KeyAction> actions = new HashMap<>();

    static {
        actions.put(NativeKeyEvent.VC_ESCAPE, new ExitGame());
        actions.put(NativeKeyEvent.VC_MINUS, new SlowerGame());
        actions.put(NativeKeyEvent.VC_OPEN_BRACKET, new SlowerGame());
        actions.put(NativeKeyEvent.VC_EQUALS, new FasterGame());
        actions.put(NativeKeyEvent.VC_CLOSE_BRACKET, new FasterGame());
        actions.put(NativeKeyEvent.VC_C, new ToggleCamera());
        actions.put(NativeKeyEvent.VC_P, new TogglePainting());
        actions.put(NativeKeyEvent.VC_V, new ToggleVisualization());
        actions.put(NativeKeyEvent.VC_SPACE, new PauseGame());
        actions.put(NativeKeyEvent.VC_BACKQUOTE, new PauseGame());
        actions.put(NativeKeyEvent.VC_PAUSE, new PauseGame());
        // Both Ctrl keys report VC_CONTROL (left/right told apart only by
        // getKeyLocation, which the registry ignores on purpose).
        actions.put(NativeKeyEvent.VC_CONTROL, new PauseGame());
        actions.put(NativeKeyEvent.VC_1, new Speed1());
        actions.put(NativeKeyEvent.VC_2, new Speed2());
        actions.put(NativeKeyEvent.VC_3, new Speed3());
        actions.put(NativeKeyEvent.VC_4, new Speed4());
        actions.put(NativeKeyEvent.VC_5, new Speed5());
        actions.put(NativeKeyEvent.VC_6, new Speed6());
        actions.put(NativeKeyEvent.VC_7, new Speed7());
        actions.put(NativeKeyEvent.VC_8, new Speed8());
        actions.put(NativeKeyEvent.VC_9, new Speed9());
        actions.put(NativeKeyEvent.VC_0, new ToggleGui());
    }

    private Keyboard() {
    }

    /** Start the global hook. Safe to call when no display exists: shortcuts just stay dead. */
    public static void listen(Mode keyboardMode) {
        mode = keyboardMode;
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            System.err.println("[keyboard] Global hook unavailable (" + e.getMessage() + "); shortcuts disabled.");
            return;
        }
        GlobalScreen.addNativeKeyListener(new Keyboard());
        active = true;
        System.out.println("[keyboard] Global shortcuts active (" + keyboardMode + ").");
    }

    /** Bot side: execute keys the supervisor forwarded. Call every frame; no-op when inactive. */
    public static void drainRelayedKeys() {
        if (!active || mode != Mode.BOT) return;
        for (int[] code : KeyRelay.drain()) {
            dispatch(code[0], code.length > 1 ? code[1] : 1);
        }
    }

    static void dispatch(int keyCode, int keyLocation) {
        if (exiting) return;
        if (mode == Mode.SUPERVISOR && keyCode != NativeKeyEvent.VC_ESCAPE) {
            KeyRelay.send(keyCode, keyLocation);
            return;
        }
        KeyAction action = actions.get(keyCode);
        if (action != null) {
            try {
                action.execute();
            } catch (LinkageError | RuntimeException e) {
                if (!exiting) throw e;
            }
        }
    }

    public static void beginExit() {
        exiting = true;
    }

    public static boolean isSupervisor() {
        return mode == Mode.SUPERVISOR;
    }

    /** True once a global hook was registered (local dev launchers only). */
    public static boolean isActive() {
        return active;
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        dispatch(e.getKeyCode(), e.getKeyLocation());
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
    }
}
