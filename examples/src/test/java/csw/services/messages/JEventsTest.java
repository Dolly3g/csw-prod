package csw.services.messages;

import csw.messages.ccs.events.*;
import csw.messages.javadsl.JUnits;
import csw.messages.params.formats.JavaJsonSupport;
import csw.messages.params.generics.JKeyTypes;
import csw.messages.params.generics.Key;
import csw.messages.params.generics.Parameter;
import csw.messages.params.models.MatrixData;
import csw.messages.params.models.ObsId;
import csw.messages.params.models.Prefix;
import csw.messages.params.models.RaDec;
import org.junit.Assert;
import org.junit.Test;
import play.api.libs.json.JsValue;
import play.api.libs.json.Json;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class JEventsTest {

    @Test
    public void showUsageOfEventTime() {
        //#eventtime

        //apply returns current time in UTC
        EventTime now = EventTime.apply();

        //using constructor
        EventTime anHourAgo = new EventTime(Instant.now().minusSeconds(3600));

        //return current time in UTC
        EventTime currentTime = EventTime.toCurrent();

        //some past time using utility function
        EventTime aDayAgo = EventTime.toEventTime(Instant.now().minusSeconds(86400));

        //#eventtime

        //validations
        Assert.assertTrue(now.time().isAfter(anHourAgo.time()));
        Assert.assertTrue(anHourAgo.time().isAfter(aDayAgo.time()));
        Assert.assertTrue(currentTime.time().isAfter(anHourAgo.time()));
    }

    @Test
    public void showUsageOfEventInfo() {
        //#eventinfo
        //with only a subsystem, time will default to now
        EventInfo eventInfo1 = EventInfo.apply("wfos.blue.filter");

        //given subsystem and time is now
        EventInfo eventInfo2 = EventInfo.apply("wfos.blue.filter", EventTime.apply(Instant.now()));

        //supply subsystem, time, ObsId
        EventInfo eventInfo3 = EventInfo.apply("wfos.blue.filter", EventTime.apply(), ObsId.apply("Obs001"));

        //with all values
        EventInfo eventInfo4 = EventInfo.apply(new Prefix("wfos.prog.cloudcover"), EventTime.apply(), ObsId.apply("Obs001").asOption(), UUID.randomUUID().toString());
        //#eventinfo

        //validations
        Assert.assertEquals(eventInfo1, eventInfo2);
        Assert.assertNotEquals(eventInfo3, eventInfo4);
    }

    @Test
    public void showUsageOfStatusEvent() {
        //#statusevent
        //keys
        Key<Integer> k1 = JKeyTypes.IntKey().make("encoder");
        Key<Integer> k2 = JKeyTypes.IntKey().make("windspeed");
        Key<String> k3 = JKeyTypes.StringKey().make("filter");
        Key<Integer> k4 = JKeyTypes.IntKey().make("notUsed");

        //prefixes
        String ck1 = "wfos.prog.cloudcover";
        String ck3 = "wfos.red.detector";

        //parameters
        Parameter<Integer> p1 = k1.set(22);
        Parameter<Integer> p2 = k2.set(44);
        Parameter<String> p3 = k3.set("A", "B", "C", "D");

        //Create StatusEvent using madd
        StatusEvent se1 = new StatusEvent(ck1).madd(p1, p2);
        //Create StatusEvent using add
        StatusEvent se2 = new StatusEvent(ck3).add(p1).add(p2);
        //Create StatusEvent and use add
        StatusEvent se3 = new StatusEvent(ck3).add(p1).add(p2).add(p3);

        //access keys
        Boolean k1Exists = se1.exists(k1); //true

        //access Parameters
        Optional<Parameter<Integer>> p4 = se1.jGet(k1);

        //access values
        List<Integer> v1 = se1.jGet(k1).get().jValues();
        List<Integer> v2 = se2.parameter(k2).jValues();
        //k4 is missing
        Set<String> missingKeys = se3.jMissingKeys(k1, k2, k3, k4);

        //remove keys
        StatusEvent se4 = se3.remove(k3);
        //#statusevent

        Assert.assertTrue(k1Exists);
        Assert.assertTrue(p4.get() == p1);
        Assert.assertEquals(new HashSet<>(Arrays.asList(22)), new HashSet<>(v1));
        Assert.assertEquals(new HashSet<>(Arrays.asList(44)), new HashSet<>(v2));
        Assert.assertEquals(new HashSet<>(Arrays.asList(missingKeys)), new HashSet<>(Arrays.asList(missingKeys)));
        Assert.assertEquals(se2, se4);
    }

    @Test
    public void showUsageOfObserveEvent() {
        //#observeevent
        //keys
        Key<Integer> k1 = JKeyTypes.IntKey().make("encoder");
        Key<Integer> k2 = JKeyTypes.IntKey().make("windspeed");
        Key<String> k3 = JKeyTypes.StringKey().make("filter");
        Key<Integer> k4 = JKeyTypes.IntKey().make("notUsed");

        //prefixes
        String ck1 = "wfos.prog.cloudcover";
        String ck3 = "wfos.red.detector";

        //parameters
        Parameter<Integer> p1 = k1.set(22);
        Parameter<Integer> p2 = k2.set(44);
        Parameter<String> p3 = k3.set("A", "B", "C", "D");

        //Create ObserveEvent using madd
        ObserveEvent oc1 = new ObserveEvent(ck1).madd(p1, p2);
        //Create ObserveEvent using add
        ObserveEvent oc2 = new ObserveEvent(ck3).add(p1).add(p2);
        //Create ObserveEvent and use add
        ObserveEvent oc3 = new ObserveEvent(ck3).add(p1).add(p2).add(p3);

        //access keys
        Boolean k1Exists = oc1.exists(k1); //true

        //access Parameters
        Optional<Parameter<Integer>> p4 = oc1.jGet(k1);

        //access values
        List<Integer> v1 = oc1.jGet(k1).get().jValues();
        List<Integer> v2 = oc2.parameter(k2).jValues();
        //k4 is missing
        Set<String> missingKeys = oc3.jMissingKeys(k1, k2, k3, k4);

        //remove keys
        ObserveEvent oc4 = oc3.remove(k3);
        //#observeevent

        Assert.assertTrue(k1Exists);
        Assert.assertTrue(p4.get() == p1);
        Assert.assertEquals(new HashSet<>(Arrays.asList(22)), new HashSet<>(v1));
        Assert.assertEquals(new HashSet<>(Arrays.asList(44)), new HashSet<>(v2));
        Assert.assertEquals(new HashSet<>(Arrays.asList(missingKeys)), new HashSet<>(Arrays.asList(missingKeys)));
        Assert.assertEquals(oc2, oc4);
    }

    @Test
    public void showUsageOfSystemEvent() {
        //#systemevent
        //keys
        Key<Integer> k1 = JKeyTypes.IntKey().make("encoder");
        Key<Integer> k2 = JKeyTypes.IntKey().make("windspeed");
        Key<String> k3 = JKeyTypes.StringKey().make("filter");
        Key<Integer> k4 = JKeyTypes.IntKey().make("notUsed");

        //prefixes
        String ck1 = "wfos.prog.cloudcover";
        String ck3 = "wfos.red.detector";

        //parameters
        Parameter<Integer> p1 = k1.set(22);
        Parameter<Integer> p2 = k2.set(44);
        Parameter<String> p3 = k3.set("A", "B", "C", "D");

        //Create SystemEvent using madd
        SystemEvent se1 = new SystemEvent(ck1).madd(p1, p2);
        //Create SystemEvent using add
        SystemEvent se2 = new SystemEvent(ck3).add(p1).add(p2);
        //Create SystemEvent and use add
        SystemEvent se3 = new SystemEvent(ck3).add(p1).add(p2).add(p3);

        //access keys
        Boolean k1Exists = se1.exists(k1); //true

        //access Parameters
        Optional<Parameter<Integer>> p4 = se1.jGet(k1);

        //access values
        List<Integer> v1 = se1.jGet(k1).get().jValues();
        List<Integer> v2 = se2.parameter(k2).jValues();
        //k4 is missing
        Set<String> missingKeys = se3.jMissingKeys(k1, k2, k3, k4);

        //remove keys
        SystemEvent oc4 = se3.remove(k3);
        //#systemevent

        Assert.assertTrue(k1Exists);
        Assert.assertTrue(p4.get() == p1);
        Assert.assertEquals(new HashSet<>(Arrays.asList(22)), new HashSet<>(v1));
        Assert.assertEquals(new HashSet<>(Arrays.asList(44)), new HashSet<>(v2));
        Assert.assertEquals(new HashSet<>(Arrays.asList(missingKeys)), new HashSet<>(Arrays.asList(missingKeys)));
        Assert.assertEquals(se2, oc4);
    }

    @Test
    public void showUsageOfJsonSerialization() {
        //#json-serialization
        //key
        Key<MatrixData<Double>> k1 = JKeyTypes.DoubleMatrixKey().make("myMatrix");

        //values
        Double[][] doubles = {{1.0, 2.0, 3.0}, {4.1, 5.1, 6.1}, {7.2, 8.2, 9.2}};
        MatrixData<Double> m1 = MatrixData.fromJavaArrays(Double.class, doubles);

        //parameter
        Parameter<MatrixData<Double>> i1 = k1.set(m1);

        //events
        StatusEvent statusEvent = new StatusEvent("wfos.blue.filter").add(i1);
        ObserveEvent observeEvent = new ObserveEvent("wfos.blue.filter").add(i1);
        SystemEvent systemEvent = new SystemEvent("wfos.blue.filter").add(i1);

        //json support - write
        JsValue statusJson = JavaJsonSupport.writeEvent(statusEvent);
        JsValue observeJson = JavaJsonSupport.writeEvent(observeEvent);
        JsValue systemJson = JavaJsonSupport.writeEvent(systemEvent);

        //optionally prettify
        String str = Json.prettyPrint(statusJson);

        //construct DemandState from string
        StatusEvent statusFromPrettyStr = JavaJsonSupport.readEvent(StatusEvent.class, Json.parse(str));

        //json support - read
        ObserveEvent observeEvent1 = JavaJsonSupport.readEvent(ObserveEvent.class, observeJson);
        SystemEvent systemEvent1 = JavaJsonSupport.readEvent(SystemEvent.class, systemJson);
        //#json-serialization

        //validations
        Assert.assertTrue(statusEvent.equals(statusFromPrettyStr));
        Assert.assertTrue(observeEvent.equals(observeEvent1));
        Assert.assertTrue(systemEvent.equals(systemEvent1));
    }

    @Test
    public void showUniqueKeyConstraintExample() {
        //#unique-key
        //keys
        Key<Integer> encoderKey = JKeyTypes.IntKey().make("encoder");
        Key<Integer> filterKey = JKeyTypes.IntKey().make("filter");
        Key<Integer> miscKey = JKeyTypes.IntKey().make("misc.");

        //prefix
        String prefix = "wfos.blue.filter";

        //params
        Parameter<Integer> encParam1 = encoderKey.set(1);
        Parameter<Integer> encParam2 = encoderKey.set(2);
        Parameter<Integer> encParam3 = encoderKey.set(3);

        Parameter<Integer> filterParam1 = filterKey.set(1);
        Parameter<Integer> filterParam2 = filterKey.set(2);
        Parameter<Integer> filterParam3 = filterKey.set(3);

        Parameter<Integer> miscParam1 = miscKey.set(100);

        //StatusEvent with duplicate key via madd
        StatusEvent event = new StatusEvent(prefix).madd(
                encParam1,
                encParam2,
                encParam3,
                filterParam1,
                filterParam2,
                filterParam3);
        //four duplicate keys are removed; now contains one Encoder and one Filter key
        List<String> uniqueKeys1 = event.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toList());

        //try adding duplicate keys via add + madd
        StatusEvent changedEvent = event.add(encParam3).madd(filterParam1, filterParam2, filterParam3);
        //duplicate keys will not be added. Should contain one Encoder and one Filter key
        List<String> uniqueKeys2 = changedEvent.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toList());

        //miscKey(unique) will be added; encoderKey(duplicate) will not be added
        StatusEvent finalEvent = changedEvent.madd(miscParam1, encParam1);
        //now contains encoderKey, filterKey, miscKey
        List<String> uniqueKeys3 = finalEvent.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toList());
        //#unique-key

        //validations
        Assert.assertEquals(new HashSet<>(uniqueKeys1), new HashSet<>(Arrays.asList(encoderKey.keyName(), filterKey.keyName())));
        Assert.assertEquals(new HashSet<>(uniqueKeys2), new HashSet<>(Arrays.asList(encoderKey.keyName(), filterKey.keyName())));
        Assert.assertEquals(new HashSet<>(uniqueKeys3), new HashSet<>(Arrays.asList(encoderKey.keyName(), filterKey.keyName(), miscKey.keyName())));
    }

    @Test
    public void showUsageOfProtobuf() {
        //#protobuf

        //Some variety in EventInfo
        EventInfo eventInfo1 = EventInfo.apply("wfos.blue.filter");
        EventInfo eventInfo2 = EventInfo.apply(
                "wfos.blue.filter",
                EventTime.apply(Instant.now()));
        EventInfo eventInfo3 = EventInfo.apply(
                "wfos.blue.filter",
                EventTime.apply(),
                ObsId.apply("Obs001"));
        EventInfo eventInfo4 = EventInfo.apply(
                new Prefix("wfos.prog.cloudcover"),
                EventTime.apply(),
                ObsId.apply("Obs001").asOption(),
                UUID.randomUUID().toString());

        //Key
        Key<RaDec> raDecKey = JKeyTypes.RaDecKey().make("raDecKey");

        //values
        RaDec raDec1 = new RaDec(10.20, 40.20);
        RaDec raDec2 = new RaDec(100.20, 400.20);

        //parameters
        Parameter<RaDec> param = raDecKey.set(raDec1, raDec2).withUnits(JUnits.arcmin);

        //events
        StatusEvent statusEvent = StatusEvent.from(eventInfo1).add(param);
        ObserveEvent observeEvent = ObserveEvent.from(eventInfo2).add(param);
        SystemEvent systemEvent1 = SystemEvent.from(eventInfo3).add(param);
        SystemEvent systemEvent2 = SystemEvent.from(eventInfo4).add(param);

        //convert events to protobuf bytestring
        byte[] byteArray1 = statusEvent.toPb();
        byte[] byteArray2 = observeEvent.toPb();
        byte[] byteArray3 = systemEvent1.toPb();
        byte[] byteArray4 = systemEvent2.toPb();

        //convert protobuf bytestring to events
        StatusEvent pbStatusEvent = StatusEvent.fromPb(byteArray1);
        ObserveEvent pbObserveEvent = ObserveEvent.fromPb(byteArray2);
        SystemEvent pbSystemEvent1 = SystemEvent.fromPb(byteArray3);
        SystemEvent pbSystemEvent2 = SystemEvent.fromPb(byteArray4);
        //#protobuf

        //validations
        Assert.assertEquals(pbStatusEvent, statusEvent);
        Assert.assertEquals(pbObserveEvent, observeEvent);
        Assert.assertEquals(pbSystemEvent1, systemEvent1);
        Assert.assertEquals(pbSystemEvent2, systemEvent2);
    }
}
