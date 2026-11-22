package Micro.Actions.Protoss.ReaverShuttle

/**
 * Stalled-duel board (pure composition for the suite): a hull holding a
 * static inside its own gun with no shot for six seconds is not dueling,
 * it is milling - the order already votes BoardRide with the ride in
 * reach, but the rescue legs all decline (no active scarab, no wound
 * band, targets present), so without this leg the hull walks at a
 * hovering ride forever and never loads. Same three facts the arbiter
 * resolves on, so the vote and the leg cannot split again.
 */
object BoardStalled {

  def shouldBoard(stalledShot: Boolean, staticFoeInGun: Boolean, rideInReach: Boolean): Boolean =
    stalledShot && staticFoeInGun && rideInReach
}
