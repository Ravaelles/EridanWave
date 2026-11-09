package Micro.Coordination.Pushing.Explosions

import Micro.Coordination.Pushing.{CircularPush, TrafficPriorities}
import Mathematics.Points.Pixel

class ExplosionNuke(pixel: Pixel) extends CircularPush(TrafficPriorities.Dive, pixel, 256)
