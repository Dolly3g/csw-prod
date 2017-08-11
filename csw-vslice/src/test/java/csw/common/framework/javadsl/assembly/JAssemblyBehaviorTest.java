package csw.common.framework.javadsl.assembly;

import akka.typed.ActorRef;
import akka.typed.ActorSystem;
import akka.typed.Behavior;
import akka.typed.Props;
import akka.typed.javadsl.ActorContext;
import akka.typed.scaladsl.Actor;
import akka.typed.testkit.TestKitSettings;
import akka.typed.testkit.scaladsl.TestProbe;
import akka.util.Timeout;
import csw.common.components.assembly.AssemblyDomainMsg;
import csw.common.framework.javadsl.JComponentHandlers;
import csw.common.framework.javadsl.JComponentWiring;
import csw.common.framework.javadsl.commons.JClassTag;
import csw.common.framework.models.*;
import csw.param.states.CurrentState;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import scala.runtime.BoxedUnit;
import scala.runtime.Nothing$;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JAssemblyBehaviorTest {
    private static ActorSystem system = ActorSystem.create(Actor.empty(), "Assembly");
    private static TestKitSettings settings = TestKitSettings.apply(system);

    private JComponentWiring getSampleJAssemblyFactory(JComponentHandlers assemblyHandlers) {
        return new JComponentWiring<AssemblyDomainMsg>(AssemblyDomainMsg.class) {

            @Override
            public JComponentHandlers<AssemblyDomainMsg> make(ActorContext<ComponentMsg> ctx, ComponentInfo componentInfo, ActorRef<PubSub.PublisherMsg<CurrentState>> pubSubRef) {
                return assemblyHandlers;
            }
        };
    }

    private ComponentInfo.AssemblyInfo assemblyInfo = new ComponentInfo.AssemblyInfo("trombone",
            "wfos",
            "csw.common.components.assembly.SampleAssembly",
            LocationServiceUsages.JDoNotRegister(),
            null,
            null);

    @AfterClass
    public static void afterAll() throws Exception {
        Await.result(system.terminate(), Duration.create(5, "seconds"));
    }

    @Test
    public void testAssemblyBehavior() throws Exception {

        JComponentHandlers<AssemblyDomainMsg> sampleAssemblyHandler = Mockito.mock(JComponentHandlers.class);
        when(sampleAssemblyHandler.initialize()).thenCallRealMethod();
        when(sampleAssemblyHandler.jInitialize()).thenReturn(CompletableFuture.completedFuture(
                BoxedUnit.UNIT
        ));

        TestProbe<FromComponentLifecycleMessage> supervisorProbe = TestProbe.apply(system, settings);

        Timeout seconds = Timeout.durationToTimeout(FiniteDuration.apply(5, "seconds"));
        Behavior<Nothing$> behavior = getSampleJAssemblyFactory(sampleAssemblyHandler).compBehavior(assemblyInfo, supervisorProbe.ref(), null);
        Future<ActorRef> assembly = system.<Nothing$>systemActorOf(behavior, "assembly", Props.empty(), seconds);
        FiniteDuration seconds1 = Duration.create(5, "seconds");
        ActorRef assemblyRef = Await.result(assembly, seconds1);

        SupervisorIdleMsg.Initialized initialized = supervisorProbe.expectMsgType(JClassTag.make(SupervisorIdleMsg.Initialized.class));
        Assert.assertEquals(assemblyRef, initialized.componentRef());

        initialized.componentRef().tell(InitialMsg.Run$.MODULE$);
        SupervisorIdleMsg.Running running = supervisorProbe.expectMsgType(JClassTag.make(SupervisorIdleMsg.Running.class));
        verify(sampleAssemblyHandler).onRun();
        verify(sampleAssemblyHandler).isOnline_$eq(true);

        Assert.assertEquals(assemblyRef, running.componentRef());
    }
}
