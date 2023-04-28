package com.redhat.console.integrations.splunk;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.util.json.JsonArray;
import org.apache.camel.util.json.JsonObject;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;

/**
 * Transformer to pick an Event from the events of the message.
 */
@ApplicationScoped
public class EventsSplitter implements Processor {

    public void process(Exchange exchange) throws Exception {

        Message in = exchange.getIn();
        JsonObject body = in.getBody(JsonObject.class);

        List<String> futures = new ArrayList<>();

        //                .transform().simple("{\"source\": \"eventing\", \"sourcetype\": \"Insights event\", \"event\": ${body}}")

        if (body.containsKey("events")) {
            JsonArray events = body.getCollection("events");
            for (int i = 0; i < events.size(); i++) {

                JsonObject oneBody = new JsonObject(body);

                JsonObject event = (JsonObject) events.get(i);
                JsonArray arr = new JsonArray();
                arr.add(event);

                oneBody.put("events", arr);

                JsonObject result = new JsonObject();
                result.put("event", oneBody);
                futures.add(result.toJson());

                //futures.add(oneBody.toJson());
                //.put("source", "eventing");
                //result.put("sourcetype", "Insights event");

            }
        }

        in.setBody(String.join(" ", futures));

        System.out.println(in.getBody(String.class));

    }

}
