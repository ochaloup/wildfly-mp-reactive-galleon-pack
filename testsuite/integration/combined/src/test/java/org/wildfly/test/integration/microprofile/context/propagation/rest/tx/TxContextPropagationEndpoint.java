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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
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

import org.eclipse.microprofile.context.ManagedExecutor;

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

    @Transactional
    @GET
    @Path("/transaction1")
    public CompletionStage<String> transactionTest1() throws SystemException {
        CompletableFuture<String> ret = allExecutor.completedFuture("OK");

        ContextEntity entity = new ContextEntity();
        entity.setName("Stef");
        em.persist(entity);
        Transaction t1 = tm.getTransaction();
        if (t1 == null) {
            throw new IllegalStateException("No tx");
        }
        assertEquals(1, count());

        return ret.thenApplyAsync(text -> {
            Transaction t2;
            try {
                t2 = tm.getTransaction();
            } catch (SystemException e) {
                throw new RuntimeException(e);
            }
            assertSame(t1, t2);
            assertEquals(1, count());
            return text;
        });
    }

    @Transactional
    @GET
    @Path("/transaction2")
    public CompletionStage<String> transactionTest2() throws SystemException {
        CompletableFuture<String> ret = allExecutor.completedFuture("OK");
        assertEquals(1, count());
        assertEquals(1, deleteAll());
        return ret.thenApplyAsync(x -> {
            throw new WebApplicationException(Response.status(Response.Status.CONFLICT).build());
        });
    }

    @Transactional
    @GET
    @Path("/transaction3")
    public CompletionStage<String> transactionTest3() throws SystemException {
        CompletableFuture<String> ret = allExecutor
                .failedFuture(new WebApplicationException(Response.status(Response.Status.CONFLICT).build()));
        assertEquals(1, count());
        assertEquals(1, deleteAll());
        return ret;
    }

    @Transactional
    @GET
    @Path("/transaction4")
    public String transactionTest4() throws SystemException {
        // check that the third transaction was not committed
        assertEquals(1, count());
        // now delete our entity
        assertEquals(1, deleteAll());

        return "OK";
    }

    private Long count() {
        TypedQuery<Long> query = em.createQuery("SELECT count(c) from ContextEntity c", Long.class);
        List<Long> result = query.getResultList();
        return result.get(0);
    }

    private int deleteAll() {
        Query query = em.createQuery("DELETE from ContextEntity");
        return query.executeUpdate();
    }

    private void assertSame(Object expected, Object actual) {
        if (expected != actual) {
            throw new IllegalStateException(expected + " is not the same as " + actual);
        }
    }

    private void assertEquals(int expected, int actual) {
        assertEquals((long)expected, (long)expected);
    }

    private void assertEquals(long expected, long actual) {
        if (expected != actual) {
            throw new IllegalStateException("Expected " + expected + "; got " + actual);
        }
    }

}
