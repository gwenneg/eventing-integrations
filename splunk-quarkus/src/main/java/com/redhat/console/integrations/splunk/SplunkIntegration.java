/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.console.integrations.splunk;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.console.integrations.CloudEventDecoder;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.http.base.HttpOperationFailedException;

import com.redhat.console.integrations.IntegrationsRouteBuilder;
import com.redhat.console.integrations.TargetUrlValidator;
import org.apache.camel.model.dataformat.JsonLibrary;

/**
 * The main class that does the work setting up the Camel routes. Entry point for messages is below
 * 'from(kafka(kafkaIngressTopic))' Upon success/failure a message is returned to the kafkaReturnTopic topic.
 */

/*
 * We need to register some classes for reflection here, so that
 * native compilation can work if desired.
 */
@RegisterForReflection(targets = {
        Exception.class,
        HttpOperationFailedException.class,
        IOException.class
})
@ApplicationScoped
public class SplunkIntegration extends IntegrationsRouteBuilder {

    @Inject
    EventsSplitter eventsSplitter;

    @Override
    public void configure() throws Exception {
        super.configure();
        configureHandler();
    }

    private void configureHandler() {
        // Receive messages on internal endpoint (within the same JVM)
        // named "splunk".

        rest("/internal/test")
                .post()
                .consumes("application/json")
                .to("direct:handler");

        from(direct("handler"))
                .removeHeaders("*")
                .process(new CloudEventDecoder())
                .routeId("handler")
                .setProperty("targetUrl", simple("${headers.metadata[url]}"))
                .setProperty("token", simple("${headers.metadata[X-Insight-Token]}"))
                .setProperty("timeIn", simpleF("%d", System.currentTimeMillis()))
                .setProperty("source", constant("eventing"))
                .setProperty("sourcetype", constant("Insights event"))
                .setProperty("skipTlsVerify", constant(true))
                //.setProperty("skipTlsVerify", simple("${headers.metadata[trustAll] != null and headers.metadata[trustAll] == 'true'}"))
                //.process(new TargetUrlValidator()) // validate the TargetUrl to be a proper url
                .process(eventsSplitter)
                //.toD("splunk-hec:${exchangeProperty.targetUrl}/${exchangeProperty.token}?skipTlsVerify=${exchangeProperty.skipTlsVerify}&bodyOnly=true&source=${exchangeProperty.source}&sourcetype=${exchangeProperty.sourcetype}", 100)
                .toD("splunk-hec:${exchangeProperty.targetUrl}/${exchangeProperty.token}?skipTlsVerify=${exchangeProperty.skipTlsVerify}&source=${exchangeProperty.source}&sourcetype=${exchangeProperty.sourcetype}&index=redhatinsights", 100)
                .to(direct("success"));
    }
}
