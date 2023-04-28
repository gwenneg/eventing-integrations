package com.redhat.console.integrations.splunk;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.util.json.JsonArray;
import org.apache.camel.util.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This processor splits one incoming payload with multiple events into several concatenated outgoing payloads
 * with a single event each. This allows us to send multiple events to Splunk HEC with a single HTTP request.
 */
public class EventsSplitter implements Processor {

    private static final String SOURCE = "eventing";
    private static final String SOURCE_TYPE = "Insights event";

    @Override
    public void process(Exchange exchange) throws Exception {

        Message in = exchange.getIn();
        JsonObject incomingBody = in.getBody(JsonObject.class);

        // This method only alters the payload of the exchange if it contains the "events" key.
        if (incomingBody.containsKey("events")) {
            JsonArray events = incomingBody.getCollection("events");
            String outgoingBody;

            if (events.isEmpty()) {
                // If the events collection is empty, we're done.
                return;

            } else if (events.size() == 1) {
                // If there's only one event, the incoming payload simply needs to be wrapped into the data structure expected by Splunk HEC.
                outgoingBody = wrapPayload(incomingBody).toJson();

            } else {
                // Otherwise, the incoming payload needs to be split.
                List<String> wrappedPayloads = new ArrayList<>();
                for (int i = 0; i < events.size(); i++) {
                    // Each event will use the incoming payload as a base for its data.
                    JsonObject singleEventPayload = new JsonObject(incomingBody);
                    // The initial events list from that payload is replaced with a single event.
                    singleEventPayload.put("events", new JsonArray(List.of(events.get(i))));
                    // The new payload is wrapped into the data structure expected by Splunk HEC.
                    JsonObject wrappedPayload = wrapPayload(singleEventPayload);
                    // All wrapped payloads are transformed into a String and collected into a list which is eventually concatenated.
                    wrappedPayloads.add(wrappedPayload.toJson());
                }
                // It's time to concatenate everything into a single outgoing body.
                outgoingBody = String.join("", wrappedPayloads);
            }

            // The initial body is replaced with whatever this method produced.
            in.setBody(outgoingBody);
        }
    }

    /**
     * Wraps a payload into the data structure expected by Splunk HEC.
     */
    private static JsonObject wrapPayload(JsonObject payload) {
        JsonObject result = new JsonObject();
        result.put("source", SOURCE);
        result.put("sourcetype", SOURCE_TYPE);
        result.put("event", payload);
        return result;
    }
}
