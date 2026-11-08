package Tuning.Strategies

import Tuning.{TuneParam, TuningStrategy}

/** Test-only strategy proving name resolution; never used in real batches. */
class TestStrategy extends TuningStrategy {

  var knob: Double = 1.0

  override def name: String = "Test"
  override def params: Seq[TuneParam] = Seq(
    TuneParam("Test.knob", () => knob.toString, text => knob = text.toDouble, 0.0, 2.0, isInt = false))
  override def mapFile: String = "test.scx"
  override def gamesPerRound: Int = 2
  override def gameTimeoutSeconds: Int = 60
}
