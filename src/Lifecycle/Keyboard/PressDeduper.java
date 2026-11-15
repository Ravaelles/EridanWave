package Lifecycle.Keyboard;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Direct-vs-relayed press dedup (pure timestamps in, boolean out): one
 * physical press reaches the client twice when both hooks are alive -
 * instantly through its own hook and ~a frame later through the
 * supervisor relay. A relayed press is a duplicate when a direct press
 * of the same code happened within the window - matching consumes it, so
 * fast mashing still executes every press. Extracted from Keyboard so the
 * 2000 ms window and the 16-entry ring have a suite instead of a comment.
 */
public final class PressDeduper {

    private final long windowMs;
    private final int maxTrackedPerCode;
    private final Map<Integer, ArrayDeque<Long>> directPresses = new HashMap<>();

    public PressDeduper(long windowMs, int maxTrackedPerCode) {
        this.windowMs = windowMs;
        this.maxTrackedPerCode = maxTrackedPerCode;
    }

    public void recordDirectPress(int keyCode, long now) {
        ArrayDeque<Long> presses = directPresses.get(keyCode);
        if (presses == null) {
            presses = new ArrayDeque<>();
            directPresses.put(keyCode, presses);
        }
        presses.addLast(now);
        while (presses.size() > maxTrackedPerCode) presses.removeFirst();
    }

    public boolean consumeDirectTwin(int keyCode, long relayMillis) {
        ArrayDeque<Long> presses = directPresses.get(keyCode);
        if (presses == null) return false;

        Iterator<Long> it = presses.iterator();
        while (it.hasNext()) {
            long directMillis = it.next();
            if (directMillis < relayMillis - windowMs) {
                it.remove();
            } else if (Math.abs(directMillis - relayMillis) <= windowMs) {
                it.remove();

                return true;
            }
        }
        return false;
    }
}
