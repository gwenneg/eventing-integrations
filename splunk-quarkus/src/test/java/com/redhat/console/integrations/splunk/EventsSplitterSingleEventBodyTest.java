package com.redhat.console.integrations.splunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.camel.Exchange;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.Test;

import static com.redhat.console.integrations.splunk.EventsSplitter.EVENT_KEY;
import static com.redhat.console.integrations.splunk.EventsSplitter.SOURCE_KEY;
import static com.redhat.console.integrations.splunk.EventsSplitter.SOURCE_TYPE_KEY;
import static com.redhat.console.integrations.splunk.EventsSplitter.SOURCE_TYPE_VALUE;
import static com.redhat.console.integrations.splunk.EventsSplitter.SOURCE_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@TestProfile(EventsSplitterSingleEventBodyTest.class)
public class EventsSplitterSingleEventBodyTest extends CamelQuarkusTestSupport {

    private EventsSplitter eventsSplitter = new EventsSplitter();

    @Test
    void testSingleEventBody() throws Exception {

        JsonObject incomingBody = buildIncomingBody();

        Exchange exchange = createExchangeWithBody(incomingBody.encode());

        eventsSplitter.process(exchange);

        String outgoingBody = exchange.getIn().getBody(String.class);
        assertOutgoingBody(outgoingBody, Map.of("alpha", "bravo", "charlie", "delta"));
    }

    public static JsonObject buildIncomingBody() {

        JsonObject event = new JsonObject();
        event.put("alpha", "bravo");
        event.put("charlie", "delta");

        List<JsonObject> events = new ArrayList<>();
        events.add(event);

        JsonObject incomingBody = new JsonObject();
        incomingBody.put("org_id", "12345");
        incomingBody.put("bundle", "rhel");
        incomingBody.put("application", "advisor");
        incomingBody.put("event_type", "new-recommendation");
        incomingBody.put("events", new JsonArray(events));

        return incomingBody;
    }

    public static void assertOutgoingBody(String outgoingBody, Map<String, String> expectedInnerEvent) {

        JsonObject json = new JsonObject(outgoingBody);

        assertEquals(SOURCE_VALUE, json.getString(SOURCE_KEY));
        assertEquals(SOURCE_TYPE_VALUE, json.getString(SOURCE_TYPE_KEY));

        JsonObject event = json.getJsonObject(EVENT_KEY);

        assertNotNull(event);
        assertEquals("12345", event.getString("org_id"));
        assertEquals("rhel", event.getString("bundle"));
        assertEquals("advisor", event.getString("application"));
        assertEquals("new-recommendation", event.getString("event_type"));

        JsonArray events = event.getJsonArray("events");

        assertEquals(1, events.size());
        for (Map.Entry<String, String> entry : expectedInnerEvent.entrySet()) {
            assertEquals(entry.getValue(), events.getJsonObject(0).getString(entry.getKey()));
        }
    }
}
