package Micro.Actions.Protoss.ReaverShuttle

/**
 * Fetch eligibility (no game access): a shuttle fetches hailing,
 * scarab-victim and idle mates - never a hull mid-duel. A fighting
 * Reaver holds its ground instead of boarding, so adopting it glues
 * the ride to a chase that never ends and starves escort, park and
 * await - the every-frame flap the UMS twins died to. Explicit hails
 * and Scarab victims board from a fight; everyone else duels first
 * and boards after. The backup claims evaluate lazily behind the
 * no-duel gate so a pressured hull never pays for its rescue math.
 */
object ShuttleFetch {

  def shouldFetchReaver(
    hasTargets: Boolean,
    wantsPickup: Boolean,
    scarabTargeted: Boolean,
    endangered: => Boolean,
    travelsFar: => Boolean,
    pairIdle: => Boolean,
    strandedFetch: => Boolean,
    reserved: => Boolean): Boolean =
    scarabTargeted ||
      wantsPickup ||
      (! hasTargets &&
        (endangered || travelsFar || pairIdle || strandedFetch || reserved))
}
