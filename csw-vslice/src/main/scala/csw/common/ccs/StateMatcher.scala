package csw.common.ccs

import csw.param.{CurrentState, DemandState}
import csw.param.parameters.Parameter

trait StateMatcher {
  def prefix: String

  def check(current: CurrentState): Boolean
}

case class DemandMatcherAll(demand: DemandState) extends StateMatcher {
  def prefix: String = demand.prefixStr

  def check(current: CurrentState): Boolean = demand.paramSet.equals(current.paramSet)
}

case class DemandMatcher(demand: DemandState, withUnits: Boolean = false) extends StateMatcher {

  def prefix: String = demand.prefixStr

  def check(current: CurrentState): Boolean = {
    demand.paramSet.forall { di =>
      val foundItem: Option[Parameter[_]] = current.find(di)
      foundItem.fold(false)(if (withUnits) _.equals(di) else _.values.sameElements(di.values))
    }
  }
}

case class PresenceMatcher(prefix: String) extends StateMatcher {
  def check(current: CurrentState) = true
}
