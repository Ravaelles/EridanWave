package Debugging

import SimVizModel._
import java.io.FileInputStream
import java.util.zip.GZIPInputStream
import scala.util.Try

import mjson.Json
import java.io.File

/** Split from EridanSimViz: one role per file. */
object SimVizParse {

  private def jAt(j: Json, key: String): Json = {
    if (j == null) null.asInstanceOf[Json]
    else try j.at(key) catch { case _: Throwable => null.asInstanceOf[Json] }
  }
  private def getIntOpt(j: Json, key: String): Option[Int] = {
    val n = jAt(j, key)
    if (n == null) None
    else try {
      val v: java.lang.Integer = n.asInteger()
      if (v == null) None else Some(v.intValue())
    } catch { case _: Throwable => None }
  }
  private def getBoolOpt(j: Json, key: String): Option[Boolean] = {
    val n = jAt(j, key)
    if (n == null) None
    else try {
      val v: java.lang.Boolean = n.asBoolean()
      if (v == null) None else Some(v.booleanValue())
    } catch { case _: Throwable => None }
  }
  private def getStringOpt(j: Json, key: String): Option[String] = {
    val n = jAt(j, key)
    if (n == null) None
    else try {
      val v: String = n.asString()
      Option(v)
    } catch { case _: Throwable => None }
  }
  private def getInt(j: Json, key: String, default: Int): Int = getIntOpt(j, key).getOrElse(default)
  private def getBool(j: Json, key: String, default: Boolean): Boolean = getBoolOpt(j, key).getOrElse(default)
  private def getString(j: Json, key: String, default: String): String = getStringOpt(j, key).getOrElse(default)

  private[Debugging] def parseSimulation(jsonFile: File): Option[SimPack] = {
    if (!jsonFile.exists()) return None

    Try {
      def readAll(f: File): String = {
        var fis: java.io.FileInputStream = null
        var bis: java.io.BufferedInputStream = null
        try {
          fis = new FileInputStream(f)
          bis = new java.io.BufferedInputStream(fis)
          bis.mark(4)
          val b1 = bis.read()
          val b2 = bis.read()
          bis.reset()
          val isGz = (b1 == 0x1f && b2 == 0x8b)
          val in: java.io.InputStream = if (isGz) new GZIPInputStream(bis) else bis
          try scala.io.Source.fromInputStream(in)(scala.io.Codec.UTF8).mkString finally in.close()
        } finally {
          try if (bis != null) bis.close() catch { case _: Throwable => }
          try if (fis != null) fis.close() catch { case _: Throwable => }
        }
      }
      val txt = readAll(jsonFile)
      if (txt.startsWith("PW2\n") || txt.startsWith("PW2\r\n")) {
        // Support multi-simulation files: select the newest complete block ending with Z
        val blocks = {
          val lines = txt.split("\r?\n")
          val out = new scala.collection.mutable.ArrayBuffer[String]()
          var i = 0
          var cur = new StringBuilder()
          var in = false
          while (i < lines.length) {
            val line = lines(i)
            val lt = if (line == null) "" else line.trim
            if (lt == "PW2") {
              if (in) {
                // previous block without terminator: drop it
                cur = new StringBuilder()
              }
              in = true
              cur.append("PW2\n")
            } else if (in) {
              cur.append(line).append('\n')
              if (lt == "Z") {
                out += cur.toString()
                cur = new StringBuilder()
                in = false
              }
            }
            i += 1
          }
          out.toVector
        }
        if (blocks.nonEmpty) parsePW2(blocks.last) else parsePW2(txt) // fallback to whole text
      } else parseJsonSim(txt)
    }.toOption
  }

  private[Debugging] def parseJsonSim(txt: String): SimPack = {
    val j = Json.read(txt)
    val mapHash = getString(j, "mapHash", "")
    val unitsJ = Option(j.at("units")).map(_.asJsonList()).map(_.toArray(new Array[Json](0)).toVector).getOrElse(Vector.empty)
    val metas = unitsJ.flatMap { u =>
      val idOpt = getIntOpt(u, "id")
      idOpt.map { id =>
        UnitMeta(
          id = id,
          friendly = getBool(u, "friendly", default = false),
          width = getInt(u, "width", default = 16),
          height = getInt(u, "height", default = 16),
          unitType = getString(u, "type", default = ""),
          fly = getBool(u, "fly", default = false)
        )
      }
    }
    val metaById: Map[Int, UnitMeta] = metas.map(m => m.id -> m).toMap
    val framesJ = Option(j.at("frames")).map(_.asJsonList()).map(_.toArray(new Array[Json](0)).toVector).getOrElse(Vector.empty)
    val frames = framesJ.flatMap { fj =>
      val fnum = getInt(fj, "f", 0)
      val unitsArr = Option(fj.at("u")).map(_.asJsonList()).map(_.toArray(new Array[Json](0)).toVector).getOrElse(Vector.empty)
      val units = unitsArr.flatMap { uj =>
        val idOpt = getIntOpt(uj, "id")
        idOpt.flatMap { id =>
          val xOpt = getIntOpt(uj, "x")
          val yOpt = getIntOpt(uj, "y")
          val aliveOpt = getBoolOpt(uj, "alive")
          if (xOpt.isEmpty || yOpt.isEmpty || aliveOpt.isEmpty) None
          else {
            val meta: UnitMeta = metaById.getOrElse(id, UnitMeta(
              id,
              friendly = getBool(uj, "friendly", default = true),
              width    = getInt(uj, "width", default = 16),
              height   = getInt(uj, "height", default = 16),
              unitType = getString(uj, "type", default = ""),
              fly      = getBool(uj, "fly", default = false)
            ))
            val fr  = getBool(uj, "friendly", default = meta.friendly)
            val w   = getInt(uj, "width", default = meta.width)
            val h   = getInt(uj, "height", default = meta.height)
            val hp    = getIntOpt(uj, "hp").getOrElse(-1)
            val sh    = getIntOpt(uj, "sh").getOrElse(-1)
            val hpMax = getIntOpt(uj, "hpMax").getOrElse(-1)
            val shMax = getIntOpt(uj, "shMax").getOrElse(-1)
            val tgt   = getIntOpt(uj, "tgt")
            val fly   = getBool(uj, "fly", default = meta.fly)
            val cd    = getIntOpt(uj, "cd").getOrElse(-1)
            Some(UnitFrame(id, fr, xOpt.get, yOpt.get, aliveOpt.get, w, h, hp, sh, hpMax, shMax, tgt, fly, cd))
          }
        }
      }
      val attacksArr = Option(fj.at("a")).map(_.asJsonList()).map(_.toArray(new Array[Json](0)).toVector).getOrElse(Vector.empty)
      val attacks = attacksArr.flatMap { pair =>
        if (pair.isArray) {
          val arr = pair.asJsonList()
          Try((arr.get(0).asInteger(), arr.get(1).asInteger())).toOption
        } else None
      }
      val deathsArr = Option(fj.at("d")).map(_.asJsonList()).map(_.toArray(new Array[Json](0)).toVector).getOrElse(Vector.empty)
      val deaths = deathsArr.flatMap(jv => Try(jv.asInteger()).toOption)
      Some(FrameData(fnum, units, attacks, deaths))
    }
    SimPack(mapHash, metas, frames, 0)
  }

  private[Debugging] def parsePW2(txt: String): SimPack = {
    val lines = txt.split("\r?\n").toVector
    var i = 0
    var mapHash = ""
    val metasBuf = new scala.collection.mutable.ArrayBuffer[UnitMeta]()
    val metaById = new scala.collection.mutable.HashMap[Int, UnitMeta]()
    // We'll derive hpMax/shMax and geometry from JBWAPI UnitType via typeId from U lines
    case class St(var x: Int, var y: Int, var alive: Boolean, var hp: Int, var sh: Int, var tgt: Option[Int], var cd: Int)
    val last = new scala.collection.mutable.HashMap[Int, St]()
    val framesBuf = new scala.collection.mutable.ArrayBuffer[FrameData]()

    var currentFrameNum = 0
    var startGameFrame = 0
    def snapshotUnits(): Vector[UnitFrame] = {
      val out = new scala.collection.mutable.ArrayBuffer[UnitFrame]()
      val it = last.iterator
      while (it.hasNext) {
        val (id, st) = it.next()
        if (st.alive) {
          val m = metaById.getOrElse(id, UnitMeta(id, friendly = false, 16, 16, "", fly = false))
          // Derive hp/shield maxima from unit type via JBWAPI (based on resolved unitType in meta)
          val utOpt = try Option(bwapi.UnitType.valueOf(m.unitType)) catch { case _: Throwable => None }
          val hpM = utOpt.map(_.maxHitPoints()).getOrElse(-1)
          val shM = utOpt.map(_.maxShields()).getOrElse(0)
          out += UnitFrame(id, m.friendly, st.x, st.y, st.alive, m.width, m.height, st.hp, st.sh, hpM, shM, st.tgt, m.fly, st.cd)
        }
      }
      out.toVector
    }

    i = 0
    while (i < lines.length) {
      val line = lines(i)
      if (line.startsWith("H|")) {
        mapHash = line.substring(2)
      } else if (line.startsWith("U|")) {
        val f = line.substring(2).split("\\|")
        if (f.length >= 3) {
          val id = Try(f(0).toInt).getOrElse(-1)
          if (id >= 0) {
            val fr = f(1) == "1"
            if (f.length == 3) {
              // New compact format: id|friendly|typeId
              val typeId = Try(f(2).toInt).getOrElse(-1)
              val utOpt = bwapi.UnitType.values().find(_.id == typeId)
              utOpt.foreach { ut =>
                val flyInf = if (ut.getRace() == bwapi.Race.Terran && ut.isBuilding()) false else ut.isFlyer()
                val meta = UnitMeta(id, fr, ut.width(), ut.height(), ut.toString, flyInf)
                metasBuf += meta
                metaById.put(id, meta)
              }
            } else if (f.length >= 8) {
              // Legacy extended format: preserve behavior
              val w = Try(f(2).toInt).getOrElse(16)
              val h = Try(f(3).toInt).getOrElse(16)
              val tpe = if (f(4) == null) "" else f(4)
              val fly = f(5) == "1"
              val meta = UnitMeta(id, fr, w, h, tpe, fly)
              metasBuf += meta
              metaById.put(id, meta)
            }
          }
        }
      } else if (line.startsWith("S|")) {
        startGameFrame = Try(line.substring(2).toInt).getOrElse(0)
      } else if (line.startsWith("F|")) {
        currentFrameNum = Try(line.substring(2).toInt).getOrElse(0)
        // Start a new frame; we'll add after processing following lines (C/A/D) or immediately if none
        // Ensure we at least append a frame with current snapshot at the F line to match previous behavior
        // We'll delay adding until we see next F or EOF to include events collected below
        // So do nothing here
      } else if (line.startsWith("C|")) {
        val body = line.substring(2)
        if (body.nonEmpty) {
          val ents = body.split(';')
          var ei = 0
          while (ei < ents.length) {
            val e = ents(ei)
            val ff = e.split(',')
            if (ff.length >= 8) {
              val id = Try(ff(0).toInt).getOrElse(-1)
              if (id >= 0) {
                val x = Try(ff(1).toInt).getOrElse(0)
                val y = Try(ff(2).toInt).getOrElse(0)
                val alive = ff(3) == "1"
                val hp = Try(ff(4).toInt).getOrElse(-1)
                val sh = Try(ff(5).toInt).getOrElse(-1)
                val tgt = Try(ff(6).toInt).toOption
                val cd = Try(ff(7).toInt).getOrElse(-1)
                val st = last.getOrElseUpdate(id, St(x, y, alive, hp, sh, None, -1))
                st.x = x; st.y = y; st.alive = alive; st.hp = hp; st.sh = sh; st.tgt = tgt.filter(_ >= 0); st.cd = cd
              }
            }
            ei += 1
          }
        }
        // After applying changes for this frame, create a snapshot frame (we'll attach events possibly in following lines by separate data structure)
        framesBuf += FrameData(currentFrameNum, snapshotUnits(), Vector.empty, Vector.empty)
      } else if (line.startsWith("A|")) {
        val body = line.substring(2)
        // Ensure a snapshot exists for this frame even if no C| was recorded
        if (framesBuf.isEmpty || framesBuf(framesBuf.length - 1).f != currentFrameNum) {
          framesBuf += FrameData(currentFrameNum, snapshotUnits(), Vector.empty, Vector.empty)
        }
        if (framesBuf.nonEmpty) {
          val lastF = framesBuf(framesBuf.length - 1)
          val pairs = new scala.collection.mutable.ArrayBuffer[(Int, Int)]()
          val ents = if (body.isEmpty) Array.empty[String] else body.split(';')
          var k = 0
          while (k < ents.length) {
            val s = ents(k)
            val gt = s.indexOf('>')
            if (gt > 0) {
              val a = Try(s.substring(0, gt).toInt).getOrElse(-1)
              val v = Try(s.substring(gt + 1).toInt).getOrElse(-1)
              if (a >= 0 && v >= 0) pairs += ((a, v))
            }
            k += 1
          }
          framesBuf(framesBuf.length - 1) = lastF.copy(attacks = pairs.toVector)
        }
      } else if (line.startsWith("D|")) {
        val body = line.substring(2)
        // Ensure a snapshot exists for this frame even if no C| was recorded
        if (framesBuf.isEmpty || framesBuf(framesBuf.length - 1).f != currentFrameNum) {
          framesBuf += FrameData(currentFrameNum, snapshotUnits(), Vector.empty, Vector.empty)
        }
        if (framesBuf.nonEmpty) {
          val lastF = framesBuf(framesBuf.length - 1)
          val ids = new scala.collection.mutable.ArrayBuffer[Int]()
          val ents = if (body.isEmpty) Array.empty[String] else body.split(';')
          var k = 0
          while (k < ents.length) {
            val s = ents(k)
            val id = Try(s.toInt).getOrElse(-1)
            if (id >= 0) ids += id
            k += 1
          }
          framesBuf(framesBuf.length - 1) = lastF.copy(deaths = ids.toVector)
        }
      }
      i += 1
    }

    SimPack(mapHash, metasBuf.toVector, framesBuf.toVector, startGameFrame)
  }

  // Image loading/cache
}
