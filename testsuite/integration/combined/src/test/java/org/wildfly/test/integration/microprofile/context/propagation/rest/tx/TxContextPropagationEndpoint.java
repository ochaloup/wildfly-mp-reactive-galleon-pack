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
import javax.persistence.TypedQuery;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
        if (em == null) {
            throw new IllegalStateException("No EntityManager");
        }
        CompletableFuture<String> ret = allExecutor.completedFuture("OK");

        ContextEntity entity = new ContextEntity();
        entity.setName("Stef");
        em.persist(entity);
        Transaction t1 = tm.getTransaction();
        if (t1 == null) {
            throw new IllegalStateException("No tx");
        }
        long count = count();
        if (1 != count()) {
            throw new IllegalStateException("Found " + count + " entries");
        }


        return ret.thenApplyAsync(text -> {
            Transaction t2;
            try {
                t2 = tm.getTransaction();
            } catch (SystemException e) {
                throw new RuntimeException(e);
            }
            if (t1 != t2) {
                throw new IllegalStateException("Tx is not same");
            }
            long count2 = count();
            if (1 != count()) {
                throw new IllegalStateException("Found " + count2 + " entries");
            }

            return text;
        });

    }

    private Long count() {
        TypedQuery<Long> query = em.createQuery("SELECT count(c) from ContextEntity c", Long.class);
        List<Long> result = query.getResultList();
        return result.get(0);
    }

}
