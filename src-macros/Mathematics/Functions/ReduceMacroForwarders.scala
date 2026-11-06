package Mathematics.Functions

import scala.language.experimental.macros

// Macro forwarders live here (in the macros sourceset) so that recompiling
// main sources never looks like a macro definition change to zinc.
// See: zinc always assumes the API changed when a source containing
// a macro definition is recompiled, which used to invalidate half the codebase.
trait ReduceMacroForwarders {
  final def minBy[A, B](values: TraversableOnce[A])(feature: A => B)(implicit ordering: Ordering[B]): Option[A] = macro ReduceMacros.minBy[A, B]
  final def maxBy[A, B](values: TraversableOnce[A])(feature: A => B)(implicit ordering: Ordering[B]): Option[A] = macro ReduceMacros.maxBy[A, B]
}
