package com.redhat.console.integrations.splunk;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.json.JsonObject;
import org.apache.camel.Exchange;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.Test;

import static com.redhat.console.integrations.splunk.EventsSplitterSingleEventBodyTest.assertOutgoingBody;
import static com.redhat.console.integrations.splunk.EventsSplitterSingleEventBodyTest.buildIncomingBody;

@QuarkusTest
@TestProfile(EventsSplitterMultipleEventsBodyTest.class)
public class EventsSplitterMultipleEventsBodyTest extends CamelQuarkusTestSupport {

    private EventsSplitter eventsSplitter = new EventsSplitter();

    @Test
    void testMultipleEventsBody() throws Exception {

        JsonObject additionalEvent = new JsonObject();
        additionalEvent.put("echo", "foxtrot");
        additionalEvent.put("golf", "hotel");

        JsonObject incomingBody = buildIncomingBody();
        incomingBody.getJsonArray("events").add(additionalEvent);

        Exchange exchange = createExchangeWithBody(incomingBody.encode());

        eventsSplitter.process(exchange);

        String outgoingBody = exchange.getIn().getBody(String.class);

        int splitIndex = outgoingBody.indexOf("}{");
        assertOutgoingBody(outgoingBody.substring(0, splitIndex + 1), Map.of("alpha", "bravo", "charlie", "delta"));
        assertOutgoingBody(outgoingBody.substring(splitIndex + 1), Map.of("echo", "foxtrot", "golf", "hotel"));
    }
}
