package csw.services.commons.commonlogger;

import csw.services.logging.javadsl.JCommonComponentLoggerActor;

//#common-component-logger-actor
//JExampleLoggerActor is used for untyped actor java class only
public abstract class JExampleLoggerActor extends JCommonComponentLoggerActor {
    @Override
    public String componentName() {
        return "my-component-name";
    }
}
//#common-component-logger-actor