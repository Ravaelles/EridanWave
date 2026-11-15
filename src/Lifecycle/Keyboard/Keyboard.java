package Lifecycle.Keyboard;

import Lifecycle.Keyboard.actions.ExitGame;
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
     * flips on and straight back off. Matching lives in PressDeduper (with
     * its own suite); this class only wires the two dispatch paths to it.
     */
    private static final PressDeduper deduper = new PressDeduper(2000, 16);

    static {
        actions.put(NativeKeyEvent.VC_ESCAPE, new ExitGame());
        actions.put(NativeKeyEvent.VC_MINUS, GameCommands::slower);
        actions.put(3658, GameCommands::slower); // NumPad "-" reports its own raw code (no VC_ exists)
        actions.put(NativeKeyEvent.VC_OPEN_BRACKET, GameCommands::slower);
        actions.put(NativeKeyEvent.VC_EQUALS, GameCommands::faster);
        actions.put(3662, GameCommands::faster); // NumPad "+" reports its own raw code (no VC_ exists)
        actions.put(NativeKeyEvent.VC_CLOSE_BRACKET, GameCommands::faster);
        actions.put(NativeKeyEvent.VC_C, GameCommands::toggleCamera);
        actions.put(NativeKeyEvent.VC_P, GameCommands::togglePainting);
        actions.put(NativeKeyEvent.VC_V, GameCommands::toggleVisualization);
        actions.put(NativeKeyEvent.VC_SPACE, GameCommands::togglePause);
        actions.put(NativeKeyEvent.VC_BACKQUOTE, GameCommands::togglePause);
        actions.put(NativeKeyEvent.VC_PAUSE, GameCommands::togglePause);
        // Both Ctrl keys report VC_CONTROL (left, 29); right Ctrl reports
        // its own raw code 3665 instead (measured - which is why a location-based
        // gate would kill it). Same shapes as Atlantis dispatches on.
        actions.put(NativeKeyEvent.VC_CONTROL, GameCommands::togglePause);
        actions.put(3665, GameCommands::togglePause);
        // Keys "1"-"9" jump straight to speedLadder rungs 0-8 (the pairs the
        // deleted Speed1..Speed9 classes used to carry one per file).
        final int[][] presets = {{20, 0}, {0, 0}, {0, 10}, {0, 20}, {0, 40}, {0, 80}, {0, 150}, {0, 250}, {0, 500}};
        final int[] digits = {
            NativeKeyEvent.VC_1, NativeKeyEvent.VC_2, NativeKeyEvent.VC_3,
            NativeKeyEvent.VC_4, NativeKeyEvent.VC_5, NativeKeyEvent.VC_6,
            NativeKeyEvent.VC_7, NativeKeyEvent.VC_8, NativeKeyEvent.VC_9 };
        for (int i = 0; i < digits.length; i++) {
            final int speed = presets[i][0];
            final int skip = presets[i][1];
            actions.put(digits[i], () -> GameCommands.setSpeed(speed, skip));
        }
        actions.put(NativeKeyEvent.VC_0, GameCommands::toggleGui);
    }

    private Keyboard() {
    }

    /** Start the global hook. Safe to call when no display exists: shortcuts just stay dead. */
    public static void listen(Mode keyboardMode) {
        mode = keyboardMode;
        relayDrainAllowed = (keyboardMode == Mode.BOT);
        if (keyboardMode == Mode.BOT) startDraining();
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

    /** Bot side: execute keys the supervisor forwarded. Called every frame and by the drain daemon. */
    public static void drainRelayedKeys() {
        if (mode != Mode.BOT || !relayDrainAllowed) return;

        for (long[] code : KeyRelay.drain()) {
            dispatchRelayed((int) code[0], code.length > 1 ? (int) code[1] : 1, code.length > 2 ? code[2] : 0);
        }
    }

    private static volatile boolean draining = false;

    /**
     * Relay consumer independent of the frame loop: the game delivers no
     * onFrame callbacks while engine-paused, so frame-driven draining would
     * starve exactly when unpausing is needed most. Polls every 100 ms like
     * the Atlantis relay thread.
     */
    public static void startDraining() {
        if (draining) return;

        draining = true;
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    drainRelayedKeys();
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    return;
                } catch (RuntimeException | LinkageError e) {
                    // Dispatch runs game code off the game thread; never let the drain die.
                }
            }
        }, "key-relay-drain");
        thread.setDaemon(true);
        thread.start();
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
        deduper.recordDirectPress(keyCode, now);
    }

    private static boolean consumeDirectTwin(int keyCode, long relayMillis) {
        return deduper.consumeDirectTwin(keyCode, relayMillis);
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


