package csw.services.commons.immutablelogger;

import akka.typed.Behavior;
import akka.typed.javadsl.Actor;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JComponentLoggerImmutable;

//#component-logger
public class JComponentImmutableActorLogger {

    public static <T> Behavior<T> behavior(String componentName) {
        return Actor.immutable((ctx, msg) -> {

            //JComponentImmutableActorLogger.class will appear against class tag in log statements
            ILogger log = JComponentLoggerImmutable.getLogger(ctx, componentName, JComponentImmutableActorLogger.class);

            return Actor.same();
        });
    }

}
//#component-logger