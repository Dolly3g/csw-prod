package csw.messages.params.states;

import csw.messages.ccs.commands.Setup;
import csw.messages.params.generics.JKeyTypes;
import csw.messages.params.generics.Key;
import csw.messages.params.generics.Parameter;
import csw.messages.params.models.ObsId;
import csw.messages.params.models.Prefix;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-185: Easy to Use Syntax/Api
public class JSateVariableTest {

    private final Key<Integer> encoderIntKey = JKeyTypes.IntKey().make("encoder");
    private final Key<String> epochStringKey = JKeyTypes.StringKey().make("epoch");
    private final Key<Integer> epochIntKey = JKeyTypes.IntKey().make("epoch");

    private final Parameter<Integer> encoderParam = encoderIntKey.set(22, 33);
    private final Parameter<String> epochStringParam = epochStringKey.set("A", "B");

    private final String prefix = "wfos.red.detector";
    private final ObsId obsId = new ObsId("obsId");

    @Test
    public void shouldAbleToCreateCurrentState() {
        CurrentState currentState = new CurrentState(prefix).add(encoderParam).add(epochStringParam);

        // typeName and prefix
        Assert.assertEquals(CurrentState.class.getSimpleName(), currentState.typeName());
        Assert.assertEquals(new Prefix(prefix), currentState.prefix());

        // exists
        Assert.assertTrue(currentState.exists(epochStringKey));
        Assert.assertFalse(currentState.exists(epochIntKey));

        // jParamSet
        HashSet<Parameter<?>> expectedParamSet = new HashSet<>(Arrays.asList(encoderParam, epochStringParam));
        Assert.assertEquals(expectedParamSet, currentState.jParamSet());
    }

    @Test
    public void shouldAbleToCreateCurrentStateFromSetup() {
        Setup setup = new Setup(obsId, prefix).add(encoderParam).add(epochStringParam);
        CurrentState currentState = new CurrentState(setup);

        // typeName and prefix
        Assert.assertEquals(CurrentState.class.getSimpleName(), currentState.typeName());
        Assert.assertEquals(new Prefix(prefix), currentState.prefix());

        // exists
        Assert.assertTrue(currentState.exists(epochStringKey));
        Assert.assertFalse(currentState.exists(epochIntKey));

        // jParamSet
        HashSet<Parameter<?>> expectedParamSet = new HashSet<>(Arrays.asList(encoderParam, epochStringParam));
        Assert.assertEquals(expectedParamSet, currentState.jParamSet());
    }

    @Test
    public void shouldAbleToCreateDemandState() {
        DemandState demandState = new DemandState(prefix).add(encoderParam).add(epochStringParam);

        // typeName and prefix
        Assert.assertEquals(DemandState.class.getSimpleName(), demandState.typeName());
        Assert.assertEquals(new Prefix(prefix), demandState.prefix());

        // exists
        Assert.assertTrue(demandState.exists(epochStringKey));
        Assert.assertFalse(demandState.exists(epochIntKey));

        // jParamSet
        HashSet<Parameter<?>> expectedParamSet = new HashSet<>(Arrays.asList(encoderParam, epochStringParam));
        Assert.assertEquals(expectedParamSet, demandState.jParamSet());
    }

    @Test
    public void shouldAbleToCreateDemandStateFromSetup() {
        Setup setup = new Setup(obsId, prefix).add(encoderParam).add(epochStringParam);
        DemandState demandState = new DemandState(setup);

        // typeName and prefix
        Assert.assertEquals(DemandState.class.getSimpleName(), demandState.typeName());
        Assert.assertEquals(new Prefix(prefix), demandState.prefix());

        // exists
        Assert.assertTrue(demandState.exists(epochStringKey));
        Assert.assertFalse(demandState.exists(epochIntKey));

        // jParamSet
        HashSet<Parameter<?>> expectedParamSet = new HashSet<>(Arrays.asList(encoderParam, epochStringParam));
        Assert.assertEquals(expectedParamSet, demandState.jParamSet());
    }

    @Test
    public void shouldAbleToMatchWithDefaultMatcher() {
        CurrentState currentState = new CurrentState(prefix).add(encoderParam).add(epochStringParam);
        DemandState demandState = new DemandState(prefix).add(encoderParam).add(epochStringParam);

        Assert.assertTrue(StateVariable.defaultMatcher(demandState, currentState));
    }

    @Test
    public void shouldAbleToCreateCurrentStatesUsingVargs() {
        CurrentState currentState1 = new CurrentState(prefix).add(encoderParam);
        CurrentState currentState2 = new CurrentState(prefix).add(epochStringParam);
        CurrentState currentState3 = new CurrentState(prefix).add(epochStringParam);
        List<CurrentState> expectedCurrentStates = Arrays.asList(currentState1, currentState2, currentState3);

        CurrentStates currentStates = StateVariable.createCurrentStates(currentState1, currentState2, currentState3);

        List<CurrentState> actualCurrentStates = currentStates.jStates();
        Assert.assertEquals(expectedCurrentStates, actualCurrentStates);
    }

    @Test
    public void shouldAbleToCreateCurrentStatesUsingList() {
        CurrentState currentState1 = new CurrentState(prefix).add(encoderParam);
        CurrentState currentState2 = new CurrentState(prefix).add(epochStringParam);
        CurrentState currentState3 = new CurrentState(prefix).add(epochStringParam);
        List<CurrentState> expectedCurrentStates = Arrays.asList(currentState1, currentState2, currentState3);

        CurrentStates currentStates = StateVariable.createCurrentStates(expectedCurrentStates);

        List<CurrentState> actualCurrentStates = currentStates.jStates();
        Assert.assertEquals(expectedCurrentStates, actualCurrentStates);
    }
}
