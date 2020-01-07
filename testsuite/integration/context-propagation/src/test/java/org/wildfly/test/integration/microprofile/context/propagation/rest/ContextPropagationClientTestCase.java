/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.microprofile.context.propagation.rest;

import java.net.URL;

import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ContextPropagationClientTestCase {
    @ArquillianResource
    URL url;


    @Deployment
    public static WebArchive getDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "ctx-ppgn-endpoint.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .setWebXML(ContextPropagationClientTestCase.class.getPackage(), "web.xml")
                .addPackage(ContextPropagationClientTestCase.class.getPackage());
        return webArchive;
    }

    @Test
    public void testTcclPropagation() {
        RestAssured.when().get(url.toExternalForm() + "context/tccl").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    // TODO we don't have RestEasy context propagation yet
    //@Test
    public void testRESTEasyContextPropagation() {
        RestAssured.when().get(url.toExternalForm() + "context/resteasy").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    // TODO we don't have servlet context propagation yet
    //@Test
    public void testServletContextPropagation() {
        RestAssured.when().get(url.toExternalForm() + "context/servlet").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void testCdiContextPropagation() {
        RestAssured.when().get(url.toExternalForm() + "context/cdi").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    // TODO for some reason the cdi context seems to get propagated anyway
    //@Test
    public void testNoCdiContextPropagation() {
        RestAssured.when().get(url.toExternalForm() + "context/nocdi").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void testTransactionPropagation() {
        RestAssured.when().get(url.toExternalForm() + "context/transaction").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

}
