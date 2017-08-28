package csw.common.framework.javadsl.integration;

import akka.typed.ActorRef;
import akka.typed.javadsl.ActorContext;
import csw.common.framework.javadsl.JComponentHandlers;
import csw.common.framework.javadsl.JComponentBehaviorFactory;
import csw.common.framework.models.ComponentInfo;
import csw.common.framework.models.ComponentMessage;
import csw.common.framework.models.PubSub;
import csw.param.states.CurrentState;

public class JSampleComponentBehaviorFactory extends JComponentBehaviorFactory<JComponentDomainMessage> {
    public JSampleComponentBehaviorFactory() {
        super(JComponentDomainMessage.class);
    }

    @Override
    public JComponentHandlers<JComponentDomainMessage> make(
            ActorContext<ComponentMessage> ctx,
            ComponentInfo componentInfo,
            ActorRef<PubSub.PublisherMessage<CurrentState>> pubSubRef) {
        return new JSampleComponentHandlers(ctx, componentInfo, pubSubRef, JComponentDomainMessage.class);
    }
}