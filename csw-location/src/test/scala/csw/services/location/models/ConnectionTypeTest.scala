package csw.services.location.models

import org.scalatest.{FunSuite, Matchers}

class ConnectionTypeTest extends FunSuite with Matchers {

  test("ConnectionType should be any one of this types : 'http', 'tcp' and 'akka'") {

    val expectedConnectionTypeValues = Set("http", "tcp", "akka")

    val actualConnectionTypeValues: Set[String] = ConnectionType.values.map(connectionType => connectionType.entryName).toSet

    actualConnectionTypeValues shouldEqual expectedConnectionTypeValues
  }

}
