/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wildfly.test.integration.microprofile.context.propagation.rest.tx;

import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;


@ApplicationScoped
public class TransactionalBean {

    @PersistenceContext(unitName = "test")
    EntityManager em;

    @Transactional(value = TxType.REQUIRES_NEW)
    // public Flowable<String> doInTxPublisher() {
    public PublisherBuilder<String> doInTxPublisher() {
        System.out.println("----> TransactionalBean count " + TestUtils.count(em));
        TestUtils.assertEquals(0, TestUtils.count(em));

        ContextEntity entity = new ContextEntity();
        entity.setName("Stef");
        em.persist(entity);

        System.out.printf("----> TransactionalBean persisted entity %d ,count: %d%n", entity.getId(), TestUtils.count(em));

         // return Flowable.fromArray("OK");
        /* return Flowable.create(source -> {
            source.onNext("OK");
            source.onComplete();
        }, BackpressureStrategy.MISSING); */
        return ReactiveStreams.of("OK");
    }

}
