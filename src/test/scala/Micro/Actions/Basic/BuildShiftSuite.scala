package Micro.Actions.Basic

import org.scalatest.funsuite.AnyFunSuite

class BuildShiftSuite extends AnyFunSuite {

  test("a fresh placement holds the assigned tile") {
    assert(BuildShift.shiftIndex(failedAttempts = 0, rememberedBlocker = false) == 0)
    assert(BuildShift.shiftIndex(failedAttempts = BuildShift.shiftAfterFrames - 1, rememberedBlocker = false) == 0)
  }

  test("stalled frames walk the spiral outward, one step per window") {
    assert(BuildShift.shiftIndex(failedAttempts = BuildShift.shiftAfterFrames, rememberedBlocker = false) == 1)
    assert(BuildShift.shiftIndex(failedAttempts = 2 * BuildShift.shiftAfterFrames, rememberedBlocker = false) == 2)
  }

  test("a remembered unseen blocker skips the wait") {
    assert(BuildShift.shiftIndex(failedAttempts = 0, rememberedBlocker = true) == 1)
    assert(BuildShift.shiftIndex(failedAttempts = 9999, rememberedBlocker = true) == 1)
  }

  test("the shift stays within the user-set base radius") {
    assert(BuildShift.shiftRadiusTiles == 5)
  }
}
