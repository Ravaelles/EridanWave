package Lifecycle.Launch

import java.nio.file.Files
import org.scalatest.funsuite.AnyFunSuite

class LaunchPathsSuite extends AnyFunSuite {

  test("system properties prefer eridanwave and trim blanks") {
    System.setProperty("eridanwave.testPropA", "  a  ")
    System.setProperty("purplewave.testPropA", "b")
    System.setProperty("eridanwave.testPropB", "   ")
    System.setProperty("purplewave.testPropB", "c")
    try {
      assert(LaunchPaths.property("testPropA") == "a")
      assert(LaunchPaths.property("testPropB") == "c")
      assert(LaunchPaths.property("testPropMissing") == null)
    } finally {
      System.clearProperty("eridanwave.testPropA")
      System.clearProperty("purplewave.testPropA")
      System.clearProperty("eridanwave.testPropB")
      System.clearProperty("purplewave.testPropB")
    }
  }

  test("explicit game root wins, else the wine prefix layout") {
    assert(LaunchPaths.gameRoot("pfx", "root") == "root")
    assert(LaunchPaths.gameRoot("pfx", null) == "pfx/drive_c/sc")
    assert(LaunchPaths.gameRoot("pfx", "  ") == "pfx/drive_c/sc")
  }

  test("explicit ini path wins, else the game root layout") {
    assert(LaunchPaths.bwapiIni("/e/bwapi.ini", "pfx", null).getPath == "/e/bwapi.ini")
    assert(LaunchPaths.bwapiIni(null, "pfx", null).getPath == "pfx/drive_c/sc/bwapi-data/bwapi.ini")
    assert(LaunchPaths.bwapiIni("  ", "pfx", "root").getPath == "root/bwapi-data/bwapi.ini")
  }

  test("wine prefix override wins, else the user home") {
    assert(LaunchPaths.defaultWinePrefix(" /p ") == "/p")
    val ambient = System.getenv("WINEPREFIX")
    if (ambient != null && ambient.trim.nonEmpty) {
      assert(LaunchPaths.defaultWinePrefix(null) == ambient.trim)
    } else {
      assert(LaunchPaths.defaultWinePrefix(null).endsWith(".wine"))
    }
  }

  test("relay file prefers the explicit path, then the shared game root file") {
    val root = Files.createTempDirectory("launchpaths").toFile
    assert(LaunchPaths.keyRelayFile("/tmp/a.txt", root.getAbsolutePath, root.getAbsolutePath).getPath == "/tmp/a.txt")

    val write = new java.io.File(root, "bwapi-data/write")
    assert(write.mkdirs())
    assert(LaunchPaths.keyRelayFile(null, root.getAbsolutePath, root.getAbsolutePath).getPath ==
      new java.io.File(write, "keys.txt").getPath)
  }
}
