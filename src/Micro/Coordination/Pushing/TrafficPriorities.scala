package Micro.Coordination.Pushing

import Debugging.Visualizations.Colors
import bwapi.Color

object TrafficPriorities {
  // Explicit ladder, lowest first: inserting a rung anywhere but the end
  // used to renumber everything below it. The order is the documentation.
  val Freedom = TrafficPriority(0, Color.Black,          "Freedom")  // No push
  val Pardon  = TrafficPriority(1, Colors.DarkBlue,      "Pardon")   // Trying to walk here
  val Nudge   = TrafficPriority(2, Colors.DarkGreen,     "Nudge")    // Trying to get into the fight
  val Bump    = TrafficPriority(3, Colors.BrightGreen,   "Bump")     // Trying to retreat
  val Shove   = TrafficPriority(4, Colors.BrightYellow,  "Shove")    // Trying to do something mission-critical (like construct a building)
  val Dodge   = TrafficPriority(5, Colors.BrightOrange,  "Dodge")    // Trying to dodge explosion
  val Dive    = TrafficPriority(6, Colors.BrightRed,     "Dive")     // Trying to dodge NUCLEAR EXPLOSION
}
