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

package org.wildfly.test.integration.microprofile.context.propagation.rest.tx;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.jboss.resteasy.annotations.Stream;
import org.reactivestreams.Publisher;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@Path("/context")
@Produces(MediaType.TEXT_PLAIN)
@RequestScoped
public class TxContextPropagationEndpoint {
    @Inject
    ManagedExecutor allExecutor;

    @PersistenceContext(unitName = "test")
    EntityManager em;

    @Inject
    TransactionManager tm;

    @Inject
    TransactionalBean txBean;

    @Transactional
    @GET
    @Path("/delete")
    public int deleteAll() {
        return TestUtils.deleteAll(em);
    }

    @Transactional
    @GET
    @Path("/transaction1")
    public CompletionStage<String> transactionTest1() throws SystemException {
        CompletableFuture<String> ret = allExecutor.completedFuture("OK");

        ContextEntity entity = new ContextEntity();
        entity.setName("Stef");
        em.persist(entity);
        Transaction t1 = tm.getTransaction();
        TestUtils.assertNotNull("No tx", t1);
        TestUtils.assertEquals(1, TestUtils.count(em));

        return ret.thenApplyAsync(text -> {
            Transaction t2;
            try {
                t2 = tm.getTransaction();
            } catch (SystemException e) {
                throw new RuntimeException(e);
            }
            TestUtils.assertSame(t1, t2);
            TestUtils.assertEquals(1, TestUtils.count(em));
            return text;
        });
    }

    @Transactional
    @GET
    @Path("/transaction2")
    public CompletionStage<String> transactionTest2() {
        CompletableFuture<String> ret = allExecutor.completedFuture("OK");
        TestUtils.assertEquals(1, TestUtils.count(em));
        TestUtils.assertEquals(1, TestUtils.deleteAll(em));
        return ret.thenApplyAsync(x -> {
            throw new WebApplicationException(Response.status(Response.Status.CONFLICT).build());
        });
    }

    @Transactional
    @GET
    @Path("/transaction3")
    public CompletionStage<String> transactionTest3() {
        CompletableFuture<String> ret = allExecutor
                .failedFuture(new WebApplicationException(Response.status(Response.Status.CONFLICT).build()));
        TestUtils.assertEquals(1, TestUtils.count(em));
        TestUtils.assertEquals(1, TestUtils.deleteAll(em));
        return ret;
    }

    @Transactional
    @GET
    @Path("/transaction4")
    public String transactionTest4() {
        // check that the third transaction was not committed
        TestUtils.assertEquals(1, TestUtils.count(em));
        // now delete our entity
        TestUtils.assertEquals(1, TestUtils.deleteAll(em));

        return "OK";
    }


    @Transactional
    @GET
    @Path("/transaction-publisher")
    @Stream(value = Stream.MODE.RAW)
    public Publisher<String> transactionPublisher() throws SystemException {
        System.out.println("----> TX Publisher ");
        ContextEntity entity = new ContextEntity();
        entity.setName("Stef");
        em.persist(entity);
        System.out.printf("----> Persisted entity %d, count: %d%n", entity.getId(), TestUtils.count(em));

        Transaction t1 = tm.getTransaction();
        System.out.println("----> Got Tx1 " + t1);
        TestUtils.assertNotNull("No tx", t1);

        // our entity
        System.out.println("----> Checking count " + TestUtils.count(em));
        TestUtils.assertEquals(1, TestUtils.count(em));

        return txBean.doInTxPublisher()
                .map(text -> {
                    System.out.printf("---> In publisher map, count %s%n", TestUtils.count(em));
                    // make sure we don't see the other transaction's entity
                    Transaction t2;
                    try {
                        t2 = tm.getTransaction();
                    } catch (SystemException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("---> map Tx " + t2);
                    if (t1 == null) {
                        System.out.println("---> t1 is null");
                    } else if (t2 == null) {
                        System.out.println("---> t2 is null");
                    } else {
                        System.out.println("---> t1.equals(t2) " + t1.equals(t2));
                        System.out.println("---> t1 == t2 " + (t1 == t2));
                    }
                    TestUtils.assertEquals(t1, t2);

                    // the map could be called several times with non-active transaction as well
                    // TestUtils.assertEquals(Status.STATUS_ACTIVE, t2.getStatus());
                    return text;
                }).buildRs();
    }

    @Transactional
    @GET
    @Path("/transaction-publisher2")
    public Publisher<String> transactionPublisher2() throws SystemException {
        Publisher<String> ret = ReactiveStreams.of("OK").buildRs();
        // now delete both entities
        TestUtils.assertEquals(2, TestUtils.deleteAll(em));
        return ret;
    }

}
