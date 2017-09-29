package csw.messages.generics

import csw.messages.models.params.{Choice, Choices}
import csw.units.Units

class GChoiceKey(name: String, keyType: KeyType[Choice], val choices: Choices) extends Key[Choice](name, keyType) {
  private def validate(xs: Seq[Choice]) =
    assert(xs.forall(choices.contains), s"Bad choice for key: $keyName which must be one of: $choices")

  override def set(xs: Choice*): Parameter[Choice] = {
    validate(xs)
    super.set(xs: _*)
  }

  override def set(v: Array[Choice], units: Units): Parameter[Choice] = {
    validate(v)
    super.set(v, units)
  }
}