package Micro.Actions.Combat.Decisioning

import org.scalatest.funsuite.AnyFunSuite

class VolleyDisciplineSuite extends AnyFunSuite {

  test("loaded hittable volleys own the frame") {
    assert(VolleyDiscipline.shouldVolley(retreating = false, cooldownLeft = 0, hasHittable = true))
    assert(VolleyDiscipline.shouldVolley(retreating = false, cooldownLeft = -3, hasHittable = true))
  }

  test("cooling guns, empty reach and retreating hulls never hold the volley") {
    assert( ! VolleyDiscipline.shouldVolley(retreating = false, cooldownLeft = 5, hasHittable = true))
    assert( ! VolleyDiscipline.shouldVolley(retreating = false, cooldownLeft = 0, hasHittable = false))
    assert( ! VolleyDiscipline.shouldVolley(retreating = true, cooldownLeft = 0, hasHittable = true))
  }
}
