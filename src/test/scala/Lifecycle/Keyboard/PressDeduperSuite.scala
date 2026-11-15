package Lifecycle.Keyboard;

import org.scalatest.funsuite.AnyFunSuite;

class PressDeduperSuite extends AnyFunSuite {

  test("a relayed twin inside the window is consumed once") {
    val deduper = new PressDeduper(2000, 16);
    deduper.recordDirectPress(30, 1000);
    assert(deduper.consumeDirectTwin(30, 1500));
    assert( ! deduper.consumeDirectTwin(30, 1500));
  }

  test("a stale direct press does not match and gets dropped") {
    val deduper = new PressDeduper(2000, 16);
    deduper.recordDirectPress(30, 1000);
    assert( ! deduper.consumeDirectTwin(30, 5000));
    assert( ! deduper.consumeDirectTwin(30, 5001));
  }

  test("other codes and the future do not match") {
    val deduper = new PressDeduper(2000, 16);
    deduper.recordDirectPress(30, 5000);
    assert( ! deduper.consumeDirectTwin(31, 5000));
    assert( ! deduper.consumeDirectTwin(30, 1000));
  }
}
