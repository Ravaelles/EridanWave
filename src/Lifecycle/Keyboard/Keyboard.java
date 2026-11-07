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
    private static volatile int lastUnknownKeyCode = -1;
    /**
     * Draining the relay file needs no native hook - only {@link #listen(Mode)}
     * having run in BOT mode. Without this the client under Wine (where hook
     * registration can fail) would silently drop every relayed key.
     * Never set on ladder: no launcher calls {@code listen} there.
     */
    private static volatile boolean relayDrainAllowed;

    private static final Map<Integer, KeyAction> actions = new HashMap<>();

    /**
     * One physical press reaches the client twice when both hooks are alive:
     * instantly through the client's own hook and ~a frame later through the
     * supervisor relay. Without dedup every toggle key (pause, camera, GUI)
     * flips on and straight back off. A relayed press is a duplicate when a
     * direct press of the same code happened within the window - matching
     * consumes it, so fast mashing still executes every press.
     */
    private static final long DUPLICATE_WINDOW_MS = 2000;
    private static final int MAX_TRACKED_PER_CODE = 16;
    private static final Map<Integer, java.util.ArrayDeque<Long>> directPresses = new HashMap<>();

    static {
        actions.put(NativeKeyEvent.VC_ESCAPE, new ExitGame());
        actions.put(NativeKeyEvent.VC_MINUS, new SlowerGame());
        actions.put(3658, new SlowerGame()); // NumPad "-" reports its own raw code (no VC_ exists)
        actions.put(NativeKeyEvent.VC_OPEN_BRACKET, new SlowerGame());
        actions.put(NativeKeyEvent.VC_EQUALS, new FasterGame());
        actions.put(3662, new FasterGame()); // NumPad "+" reports its own raw code (no VC_ exists)
        actions.put(NativeKeyEvent.VC_CLOSE_BRACKET, new FasterGame());
        actions.put(NativeKeyEvent.VC_C, new ToggleCamera());
        actions.put(NativeKeyEvent.VC_P, new TogglePainting());
        actions.put(NativeKeyEvent.VC_V, new ToggleVisualization());
        actions.put(NativeKeyEvent.VC_SPACE, new PauseGame());
        actions.put(NativeKeyEvent.VC_BACKQUOTE, new PauseGame());
        actions.put(NativeKeyEvent.VC_PAUSE, new PauseGame());
        // Both Ctrl keys report VC_CONTROL (left, 29); right Ctrl reports its
        // own raw code 3665 instead (measured - which is why a location-based
        // gate would kill it). Same shapes as Atlantis dispatches on.
        actions.put(NativeKeyEvent.VC_CONTROL, new PauseGame());
        actions.put(3665, new PauseGame());
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
        relayDrainAllowed = (keyboardMode == Mode.BOT);
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            System.err.println("[keyboard] Global hook unavailable (" + e.getMessage() + "); shortcuts disabled.");
            System.err.println("[keyboard] Relay file: " + KeyRelay.describe());
            return;
        }
        GlobalScreen.addNativeKeyListener(new Keyboard());
        active = true;
        System.out.println("[keyboard] Global shortcuts active (" + keyboardMode + ").");
        System.out.println("[keyboard] Relay file: " + KeyRelay.describe());
    }

    /** Bot side: execute keys the supervisor forwarded. Call every frame; no-op when inactive. */
    public static void drainRelayedKeys() {
        if (mode != Mode.BOT || !relayDrainAllowed) return;
        for (long[] code : KeyRelay.drain()) {
            dispatchRelayed((int) code[0], code.length > 1 ? (int) code[1] : 1, code.length > 2 ? code[2] : 0);
        }
    }

    /** A press the client's own hook saw: always executes and suppresses its relayed twin. */
    static void dispatchDirect(int keyCode, int keyLocation) {
        recordDirectPress(keyCode, System.currentTimeMillis());
        dispatch(keyCode, keyLocation);
    }

    /** A press the supervisor relayed: dropped when it duplicates a direct press. */
    static void dispatchRelayed(int keyCode, int keyLocation, long relayMillis) {
        if (relayMillis > 0 && consumeDirectTwin(keyCode, relayMillis)) return;
        dispatch(keyCode, keyLocation);
    }

    private static void recordDirectPress(int keyCode, long now) {
        java.util.ArrayDeque<Long> presses = directPresses.get(keyCode);
        if (presses == null) {
            presses = new java.util.ArrayDeque<>();
            directPresses.put(keyCode, presses);
        }
        presses.addLast(now);
        while (presses.size() > MAX_TRACKED_PER_CODE) presses.removeFirst();
    }

    private static boolean consumeDirectTwin(int keyCode, long relayMillis) {
        java.util.ArrayDeque<Long> presses = directPresses.get(keyCode);
        if (presses == null) return false;
        java.util.Iterator<Long> it = presses.iterator();
        while (it.hasNext()) {
            long directMillis = it.next();
            if (directMillis < relayMillis - DUPLICATE_WINDOW_MS) {
                it.remove();
            } else if (Math.abs(directMillis - relayMillis) <= DUPLICATE_WINDOW_MS) {
                it.remove();
                return true;
            }
        }
        return false;
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
        } else if (keyCode != lastUnknownKeyCode) {
            lastUnknownKeyCode = keyCode;
            System.out.println("[keyboard] No action for key code " + keyCode + " (location " + keyLocation + ").");
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

    /** One-line launch diagnostic for the bot log: mode, hook, drain, relay path. */
    public static String status() {
        return "mode=" + mode + " hook=" + (active ? "up" : "down")
            + " drain=" + (mode == Mode.BOT && relayDrainAllowed ? "on" : "off")
            + " relay=" + KeyRelay.describe();
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        dispatchDirect(e.getKeyCode(), e.getKeyLocation());
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
    }
}
