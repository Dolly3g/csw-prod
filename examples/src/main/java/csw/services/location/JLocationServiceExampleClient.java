package csw.services.location;

import akka.actor.*;
import akka.japi.Pair;
import akka.japi.pf.ReceiveBuilder;
import akka.stream.ActorMaterializer;
import akka.stream.KillSwitch;
import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import csw.services.location.javadsl.*;
import csw.services.location.models.*;
import csw.services.location.scaladsl.ActorSystemFactory;
import csw.services.location.models.Connection.AkkaConnection;
import csw.services.location.models.Connection.HttpConnection;
import csw.services.logging.javadsl.ILogger;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import csw.services.commons.JExampleLogger;

/**
 * An example location service client application.
 */
public class JLocationServiceExampleClient extends AbstractActor implements JExampleLogger {

    private ILogger jLogger = getLogger();

    //#create-location-service
    private ILocationService locationService = JLocationServiceFactory.make();
    //#create-location-service

    private Connection exampleConnection = LocationServiceExampleComponent.connection();

    private IRegistrationResult httpRegResult;
    private IRegistrationResult hcdRegResult;
    private IRegistrationResult assemblyRegResult;


    public JLocationServiceExampleClient() throws ExecutionException, InterruptedException {

        // EXAMPLE DEMO START

        // This demo shows some basics of using the location service.  Before running this example,
        // optionally use the location service csw-location-agent to start a redis server:
        //
        //   $ csw-location-agent --name redis --command "redis-server --port %port"
        //
        // Not only does this serve as an example for starting applications that are not written in CSW,
        // but it also will help demonstrate location filtering later in this demo.


        registerConnectionsBlocking();
        findAndResolveConnectionsBlocking();
        listingAndFilteringBlocking();
        trackingAndSubscribingBlocking();
    }

    private class AllDone {}

    private void registerConnectionsBlocking() throws ExecutionException, InterruptedException {
        //#Components-Connections-Registrations
        // dummy http connection
        HttpConnection httpConnection   = new HttpConnection(new ComponentId("configuration", JComponentType.Service));
        HttpRegistration httpRegistration = new HttpRegistration(httpConnection, 8080, "path123");
        httpRegResult = locationService.register(httpRegistration).get();

        // dummy HCD connection
        AkkaConnection hcdConnection = new AkkaConnection(new ComponentId("hcd1", JComponentType.HCD));
        AkkaRegistration hcdRegistration = new AkkaRegistration(hcdConnection, getContext().actorOf(Props.create(AbstractActor.class, () -> new AbstractActor() {
                    @Override
                    public Receive createReceive() {
                        return ReceiveBuilder.create().build();
                    }
                }),
                "my-actor-1"
        ));
        hcdRegResult = locationService.register(hcdRegistration).get();

        //. register the client "assembly" created in this example
        AkkaConnection assemblyConnection = new AkkaConnection(new ComponentId("assembly1", JComponentType.Assembly));
        AkkaRegistration assemblyRegistration = new AkkaRegistration(assemblyConnection, getSelf());
        assemblyRegResult = locationService.register(assemblyRegistration).get();
        //#Components-Connections-Registrations
    }

    private void findAndResolveConnectionsBlocking() throws ExecutionException, InterruptedException {

        //#find
        // find connection to LocationServiceExampleComponent in location service
        // [do this before starting LocationServiceExampleComponent.  this should return Future[None]]

        jLogger.info(() -> "Attempting to find connection " + exampleConnection + "...");
        Optional<Location> findResult = locationService.find(exampleConnection).get();
        if (findResult.isPresent()) {
            jLogger.info(() -> "Find result: " + connectionInfo(findResult.get().connection()));
        } else {
            jLogger.info(() -> "Find result: None");
        }
        //#find
        // Output should be:
        //    Attempting to find connection AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) ...
        //    Find result: None

        //#resolve
        // resolve connection to LocationServiceExampleComponent
        // [start LocationServiceExampleComponent after this command but before timeout]
        FiniteDuration waitForResolveLimit = new FiniteDuration(30, TimeUnit.SECONDS);
        jLogger.info(() -> "Attempting to resolve " + exampleConnection + " with a wait of " + waitForResolveLimit + "...");
        Optional<Location> resolveResult = locationService.resolve(exampleConnection, waitForResolveLimit).get();
        if (resolveResult.isPresent()) {
            jLogger.info(() -> "Resolve result: " + connectionInfo(resolveResult.get().connection()));
        } else {
            jLogger.info(() -> "Timeout waiting for location " + exampleConnection + " to resolve.");
        }
        //#resolve

        // Output should be:
        //    Attempting to resolve AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) with a wait of 30 seconds ...

        // If you then start the LocationServiceExampleComponentApp,
        // Output should be:
        //    Resolve result: LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType

        // If not,
        // Output should be:
        //    Timeout waiting for location AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly)) to resolve.

       // example code showing how to get the actorRef for remote component and send it a message
         if (resolveResult.isPresent()) {
            Location loc = resolveResult.get();
            if (loc instanceof AkkaLocation) {
                ActorRef actorRef = ((AkkaLocation)loc).actorRef();
                actorRef.tell(LocationServiceExampleComponent.ClientMessage$.MODULE$, getSelf());
            } else {
                jLogger.error(() -> "Received unexpected location type: " + loc.getClass());
            }
        }
    }

    private void listingAndFilteringBlocking() throws ExecutionException, InterruptedException {
        //#list
        // list connections in location service
        List<Location> connectionList = locationService.list().get();
        jLogger.info(() -> "All Registered Connections:");
        for (Location loc: connectionList) {
            jLogger.info(() -> "--- " + connectionInfo(loc.connection()));
        }
        //#list
        // Output should be:
        //    All Registered Connections:
        //    --- hcd1-hcd-akka, component type=HCD, connection type=AkkaType
        //    --- assembly1-assembly-akka, component type=Assembly, connection type=AkkaType
        //    --- redis-service-tcp, component type=Service, connection type=TcpType
        //    --- configuration-service-http, component type=Service, connection type=HttpType
        //    --- LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType


        //#filtering-component
        // filter connections based on component type
        List<Location> componentList = locationService.list(JComponentType.Assembly).get();
        jLogger.info(() -> "Registered Assemblies:");
        for (Location loc: componentList) {
            jLogger.info(() -> "--- " + connectionInfo(loc.connection()));
        }
        //#filtering-component

        // Output should be:
        //    Registered Assemblies:
        //    --- assembly1-assembly-akka, component type=Assembly, connection type=AkkaType
        //    --- LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType

        //#filtering-connection
        // filter connections based on connection type
        List<Location> akkaList = locationService.list(JConnectionType.AkkaType).get();
        jLogger.info(() -> "Registered Akka connections:");
        for (Location loc : akkaList) {
            jLogger.info(() -> "--- " + connectionInfo(loc.connection()));
        }
        //#filtering-connection

        // Output should be:
        //    Registered Akka connections:
        //    --- hcd1-hcd-akka, component type=HCD, connection type=AkkaType
        //    --- assembly1-assembly-akka, component type=Assembly, connection type=AkkaType
        //    --- LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType

    }

    private void trackingAndSubscribingBlocking() {
        //#tracking
        // the following two methods are examples of two ways to track a connection.
        // both are implemented but only one is really needed.

        // track connection to LocationServiceExampleComponent
        // Calls track method for example connection and forwards location messages to this actor
        Materializer mat = ActorMaterializer.create(getContext());
        jLogger.info(() -> "Starting to track " + exampleConnection);
        locationService.track(exampleConnection).toMat(Sink.actorRef(getSelf(), AllDone.class), Keep.both()).run(mat);

        //track returns a Killswitch, that can be used to turn off notifications arbitarily
        //in this case track a connection for 5 seconds, after that schedule switching off the stream
        Pair pair = (Pair)locationService.track(exampleConnection).toMat(Sink.ignore(), Keep.both()).run(mat);
        context().system().scheduler().scheduleOnce(Duration.create(5, TimeUnit.SECONDS), new Runnable() {
            @Override
            public void run() {
                ((KillSwitch)pair.first()).shutdown();
            }
        }, context().system().dispatcher());

        // subscribe to LocationServiceExampleComponent events
        jLogger.info(() -> "Starting a subscription to " + exampleConnection);
        locationService.subscribe(exampleConnection, trackingEvent -> {
            jLogger.info(() -> "subscription event");
            getSelf().tell(trackingEvent, ActorRef.noSender());
        });
        //#tracking

        // [tracking shows component unregister and re-register]



        // Output should be:
        //    Starting to track AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
        //    Starting a subscription to AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
        //    subscription event
        //    Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
        //    Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType

        // If you now stop the LocationServiceExampleComponentApp,
        // Output should be:
        //    subscription event
        //    Location removed AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))
        //    Location removed AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))

        // If you start the LocationServiceExampleComponentApp again,
        // Output should be:
        //    Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType
        //    subscription event
        //    Location updated LocationServiceExampleComponent-assembly-akka, component type=Assembly, connection type=AkkaType


    }

    private String connectionInfo(Connection connection) {
        // construct string with useful information about a connection
        return connection.name()+", component type="+connection.componentId().componentType()+", connection type="+connection.connectionType();
    }

    @Override
    public void postStop() throws ExecutionException, InterruptedException {
        //#unregister
        httpRegResult.unregister();
        hcdRegResult.unregister();
        assemblyRegResult.unregister();
        //#unregister

        //#shutdown
        locationService.shutdown().get();
        //#shutdown
    }

    @Override
    public Receive createReceive() {
        // message handler for this actor
        return receiveBuilder()
                .match(LocationUpdated.class, l -> jLogger.info(() -> "Location updated " + connectionInfo(l.connection())))
                .match(LocationRemoved.class, l -> jLogger.info(() -> "Location removed " + l.connection()))
                .match(AllDone.class, x -> jLogger.info(() -> "Tracking of " + exampleConnection + " complete."))
                .matchAny(x -> jLogger.info(() -> "Unexpected message: "+ x))
                .build();
    }

    public static void main(String[] args) {
        //#create-actor-system
        ActorSystem actorSystem = ActorSystemFactory.remote("csw-examples-locationServiceClient");
        //#create-actor-system
        actorSystem.actorOf(Props.create(JLocationServiceExampleClient.class), "LocationServiceExampleClient");

    }
}