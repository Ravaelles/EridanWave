package Information.GameSense
import Tactic.Production.core.Production

object Budgets extends Enumeration {
  val
    Worker,
    Base,
    Supply,
    Production,
    Science,
    Defense,
    War = Value
}
