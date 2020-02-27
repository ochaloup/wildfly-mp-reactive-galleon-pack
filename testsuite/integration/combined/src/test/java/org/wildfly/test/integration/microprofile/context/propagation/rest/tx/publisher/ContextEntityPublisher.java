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

package org.wildfly.test.integration.microprofile.context.propagation.rest.tx.publisher;

import org.jboss.logging.Logger;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.transaction.Transaction;
import java.util.List;

/**
 * TODO: ...
 */
public class ContextEntityPublisher implements Publisher<Long> {
    private static final Logger log = Logger.getLogger(ContextEntityPublisher.class);

    private final EntityManager em;
    private final Transaction txn;
    private final List<Long> primaryKeys;

    public ContextEntityPublisher(EntityManager em, Transaction txn) {
        if(em == null) throw new NullPointerException("em");
        if(txn == null) throw new NullPointerException("txn");

        this.em = em;
        this.txn = txn;

        // reading the primary keys that will will searched for later
        TypedQuery<Long> query = em.createQuery("SELECT id from ContextEntity c", Long.class);
        primaryKeys = query.getResultList();
        // expecting at least some data to process with
        if(primaryKeys.isEmpty()) {
            throw new IllegalStateException("There is no data for processing. It seems no ContextEntity was provided to subscribe method.");
        }
    }

    @Override
    public void subscribe(Subscriber subscriber) {
        primaryKeys.forEach(primaryKey -> subscriber.onNext(primaryKey));
        subscriber.onComplete();
    }
}