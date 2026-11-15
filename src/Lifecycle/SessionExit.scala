package Lifecycle

import Lifecycle.Launch.GameLauncherFactory

/**
 * Post-game exit policy (was EridanWave.onEnd's second half): single-match
 * runs end with the process - Wine dev sessions (Atlantis parity: the
 * supervisor waits for this JVM to die and then tears the session down;
 * without it the script sits in `while kill -0 CLIENT_PID` until Escape)
 * and one-shot scbw Docker containers (which scbw tears down after the
 * game; an idling JVM is reported as a crash after its 70 s container
 * timeout). Ladder/tournaments keep running: the next match reuses this
 * process. Guarded by frame count: an onEnd delivered without a game ever
 * running (stale score screen on attach) must not kill a fresh session.
 */
object SessionExit {

  def afterGame(): Boolean =
    (GameLauncherFactory.isWineClientJvm() || GameLauncherFactory.isScbwMatchContainer()) && With.frame > 120
}
