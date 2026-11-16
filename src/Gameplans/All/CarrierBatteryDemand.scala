package Gameplans.All

/**
 * Carrier battery demand (pure numbers, no game access): Shield
 * Batteries scale with the interceptor wing, not with the plan. Eight
 * interceptors earn a battery, twenty earn a second - below that the
 * minerals stay in the fleet. Existing batteries count at the call
 * site; this only prices the wing.
 */
object CarrierBatteryDemand {

  /** First battery at this many live interceptors. */
  val firstBatteryInterceptors: Int = 8

  /** Second battery at this many live interceptors. */
  val secondBatteryInterceptors: Int = 20

  def wantBatteries(interceptors: Int): Int =
    if (interceptors >= secondBatteryInterceptors) 2
    else if (interceptors >= firstBatteryInterceptors) 1
    else 0
}
