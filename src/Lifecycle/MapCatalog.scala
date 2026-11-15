package Lifecycle

/**
 * What we know about a map by name (one home for the UMS question): the
 * scenario-content markers that force USE_MAP_SETTINGS, and the UMS maps
 * that play like normal games anyway. BwapiIni's game-type default and
 * MapSpecificCommander's normal-map exception both read here; the
 * marker list in scripts/wine-run.sh is the same idea for bash (kept in
 * sync by hand, same as before - the script cannot call Scala).
 */
object MapCatalog {

  val umsMarkers: Seq[String] = Seq(
    "ums", "micro", "minimap", "defense", "defence", "scenario", "bullet",
    "_v_", "vsgosu", "exp_skilltest", "dragoon_v", "zeal_v", "goon_v",
    "wraith_v", "lings_v", "marines_v", "jim_v_", "mar_v_", "rebas",
    "hydraling", "m&m", "sunken", "reav_v", "probe_run", "drag_run",
    "zealdrag", "wounded", "incorner", "tanks_v", "gosu", "7th")

  val normalMapExceptions: Seq[String] = Seq(
    "ums/vsGosuComputer.scx",
    "ums/rav/vsGosuRav.scx")

  def isUmsMap(resolvedMapPath: String): Boolean = {
    if (resolvedMapPath == null) {
      return false
    }

    val lower = resolvedMapPath.toLowerCase(java.util.Locale.ROOT)
    umsMarkers.exists(lower.contains)
  }

  def isNormalMapException(mapFileName: String): Boolean = {
    // Atlantis compared for equality against its ActiveMap.name(); our
    // With.mapFileName carries the maps/ prefix the game reports, so
    // containment matches exactly the same games.
    mapFileName != null && normalMapExceptions.exists(mapFileName.contains)
  }
}
