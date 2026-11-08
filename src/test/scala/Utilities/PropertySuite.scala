package Utilities

import org.scalatest.funsuite.AnyFunSuite

class PropertySuite extends AnyFunSuite {

  test("a fresh property holds its default and is not marked set") {
    val property = new Property[Int](42)
    assert(property.get == 42)
    assert(property() == 42)
    assert(!property.isSet)
  }

  test("setting a value marks it set and returns it") {
    val property = new Property[String]("a")
    property.set("b")
    assert(property.get == "b")
    assert(property.isSet)
  }

  test("reset restores the default and clears the flag") {
    val property = new Property[Int](7)
    property.set(9)
    property.reset()
    assert(property.get == 7)
    assert(!property.isSet)
  }

  test("a property delegates to its parent while unset") {
    val parent = new Property[Int](1)
    val child = new Property[Int](2)
    child.inherit(parent)
    assert(child.get == 1)
    child.set(3)
    assert(child.get == 3, "an explicit value shadows the parent")
    assert(parent.get == 1, "shadowing never writes through")
  }

  test("inheritance chains through grandparents") {
    val grandparent = new Property[String]("root")
    val parent = new Property[String]("mid")
    val child = new Property[String]("leaf")
    parent.inherit(grandparent)
    child.inherit(parent)
    assert(child.get == "root")
  }

  test("cyclical inheritance is rejected, not hung on") {
    val property = new Property[Int](0)
    assertThrows[Exception] {
      property.inherit(property)
    }
  }
}
