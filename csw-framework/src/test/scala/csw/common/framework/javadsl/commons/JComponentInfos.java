package csw.common.framework.javadsl.commons;

import csw.common.framework.javadsl.JComponentInfo;
import csw.common.framework.models.ComponentInfo;
import csw.common.framework.models.LocationServiceUsage;
import csw.services.location.javadsl.JComponentType;

import java.util.Collections;

public class JComponentInfos {

    public static ComponentInfo jHcdInfo = JComponentInfo.from(
            "trombone",
            JComponentType.HCD,
            "wfos",
            "csw.common.framework.javadsl.components.JSampleComponentBehaviorFactory",
            LocationServiceUsage.JDoNotRegister(),
            Collections.emptySet());
}