package csw.services.location.internal

import java.time.Instant

import akka.Done
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata._
import akka.pattern.ask
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{KillSwitch, OverflowStrategy}
import akka.util.Timeout
import com.persist.logging._
import csw.services.location.commons.CswCluster
import csw.services.location.exceptions.{
  OtherLocationIsRegistered,
  RegistrationFailed,
  RegistrationListingFailed,
  UnregistrationFailed
}
import csw.services.location.internal.Registry.AllServices
import csw.services.location.internal.StreamExt.RichSource
import csw.services.location.models._
import csw.services.location.scaladsl.LocationService

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.{DurationDouble, FiniteDuration}

private[location] class LocationServiceImpl(cswCluster: CswCluster) extends LocationService { outer =>

  import cswCluster._
  implicit val timeout: Timeout = Timeout(5.seconds)

  /**
   * Register a 'connection -> location' entry in CRDT
    **/
  def register(registration: Registration): Future[RegistrationResult] = {

    //Get the location from this registration
    val location = registration.location(cswCluster.hostname)

    //Create a message handler for this connection
    val service = new Registry.Service(registration.connection)

    //Create an update message to update the value of connection key. if the current value is None or same as
    //this location then update it with this location. if it is some other location then an exception will be thrown and
    //it will be handled below by ModifyFailure.
    val updateValue = service.update {
      case r @ LWWRegister(Some(`location`) | None) => r.withValue(Some(location))
      case LWWRegister(Some(otherLocation))         => throw OtherLocationIsRegistered(location, otherLocation)
    }

    //Create a message to update connection -> location map in CRDT
    val updateRegistry = AllServices.update(_ + (registration.connection → location))

    //Send the update message for connection key to replicator. On success, send another message to update connection -> location
    //map. If that is successful then return a registrationResult for this Location. In case of any failure throw an exception.
    (replicator ? updateValue).flatMap {
      case _: UpdateSuccess[_] =>
        (replicator ? updateRegistry).map {
          case _: UpdateSuccess[_] => registrationResult(location)
          case _                   => throw RegistrationFailed(registration.connection)
        }
      case ModifyFailure(service.Key, _, cause, _) => throw cause
      case _                                       => throw RegistrationFailed(registration.connection)
    }
  }

  /**
   * Unregister the connection from CRDT
   */
  def unregister(connection: Connection): Future[Done] = {
    //Create a message handler for this connection
    val service = new Registry.Service(connection)

    //Send an update message to replicator to update the connection key with None. On success send another message to remove the
    //corresponding connection -> location entry from map. In case of any failure throw an exception otherwise return Done.
    (replicator ? service.update(_.withValue(None))).flatMap {
      case x: UpdateSuccess[_] =>
        (replicator ? AllServices.update(_ - connection)).map {
          case _: UpdateSuccess[_] => Done
          case _                   => throw UnregistrationFailed(connection)
        }
      case _ => throw UnregistrationFailed(connection)
    }
  }

  /**
   * Unregister all connections from CRDT
   * Note : This method shold be used for testing purpose only
   */
  def unregisterAll(): Future[Done] = async {
    //Get all locations registered with CRDT
    val locations = await(list)

    //for each location unregister it's corresponding connection
    await(Future.traverse(locations)(loc ⇒ unregister(loc.connection)))
    Done
  }

  /**
   * Resolves the location for a connection from the local cache
   */
  def find(connection: Connection): Future[Option[Location]] = async {
    log.trace(Map("@msg" → "in find", "time" → Instant.now().toString))
    log.debug("hello")
    await(list).find(_.connection == connection)
  }

  /**
   * Resolve a location for the given connection
   */
  override def resolve(connection: Connection, within: FiniteDuration): Future[Option[Location]] = async {
    log.info("in resolve")
    val foundInLocalCache = await(find(connection))
    if (foundInLocalCache.isDefined) foundInLocalCache else await(resolveWithin(connection, within))
  }

  /**
   * List all locations registered with CRDT
   */
  def list: Future[List[Location]] = (replicator ? AllServices.get).map {
    case x @ GetSuccess(AllServices.Key, _) => x.get(AllServices.Key).entries.values.toList
    case NotFound(AllServices.Key, _)       ⇒ List.empty
    case _                                  => throw RegistrationListingFailed
  }

  /**
   * List all locations registered for the given componentType
   */
  def list(componentType: ComponentType): Future[List[Location]] = async {
    await(list).filter(_.connection.componentId.componentType == componentType)
  }

  /**
   * List all locations registered with the given hostname
   */
  def list(hostname: String): Future[List[Location]] = async {
    await(list).filter(_.uri.getHost == hostname)
  }

  /**
   * List all locations registered with the given connection type
   */
  def list(connectionType: ConnectionType): Future[List[Location]] = async {
    await(list).filter(_.connection.connectionType == connectionType)
  }

  /**
   * Track the status of given connection
   */
  def track(connection: Connection): Source[TrackingEvent, KillSwitch] = {
    //Create a message handler for this connection
    val service = new Registry.Service(connection)
    //Get a stream that emits messages sent to the actor generated after materialization
    val source = Source.actorRef[Any](256, OverflowStrategy.dropHead).mapMaterializedValue {
      //Subscribe materialized actorRef to the changes in connection so that above stream starts emitting messages
      actorRef ⇒
        replicator ! Subscribe(service.Key, actorRef)
    }

    //Collect only the Changed events for this connection and transform it to location events.
    // If the changed event contains a Location, send LocationUpdated event.
    // If not, location must have been removed, send LocationRemoved event.
    val trackingEvents = source.collect {
      case c @ Changed(service.Key) if c.get(service.Key).value.isDefined =>
        LocationUpdated(c.get(service.Key).value.get)
      case c @ Changed(service.Key) => LocationRemoved(connection)
    }
    //Allow stream to be cancellable by giving it a KillSwitch in mat value.
    // Also, deduplicate identical messages in case multiple DeathWatch actors unregisters the same location.
    trackingEvents.cancellable.distinctUntilChanged
  }

  /**
   * Subscribe to events of a connection by providing a callback.
   */
  override def subscribe(connection: Connection, callback: TrackingEvent ⇒ Unit): KillSwitch =
    track(connection).to(Sink.foreach(callback)).run()

  /**
   * Terminate the ActorSystem and gracefully leave the akka cluster
   * Note: It is recommended not to perform any operation on LocationService after shutdown
   */
  def shutdown(): Future[Done] = cswCluster.shutdown()

  private def registrationResult(loc: Location): RegistrationResult = new RegistrationResult {
    override def location: Location = loc

    override def unregister(): Future[Done] = outer.unregister(location.connection)
  }

  private def resolveWithin(connection: Connection, waitTime: FiniteDuration): Future[Option[Location]] =
    track(connection)
      .collect {
        case LocationUpdated(location) ⇒ location
      }
      .takeWithin(waitTime)
      .runWith(Sink.headOption)
}
