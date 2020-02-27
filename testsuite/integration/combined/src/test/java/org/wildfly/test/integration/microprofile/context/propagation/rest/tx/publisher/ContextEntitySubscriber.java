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
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.wildfly.test.integration.microprofile.context.propagation.rest.tx.ContextEntity;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

public class ContextEntitySubscriber implements Subscriber<Long> {
    private static final Logger log = Logger.getLogger(ContextEntitySubscriber.class);

    private final EntityManager em;
    private final TransactionManager tm;

    public ContextEntitySubscriber(EntityManager em, TransactionManager tm) {
        this.em = em;
        this.tm = tm;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        log.infof("onSubscribe, subscription: " + subscription);
    }

    @Override
    public void onNext(Long primaryKey) {
        try {
            log.infof("On next [%d], transaction: %s", primaryKey, tm.getTransaction());
        } catch (Exception e) {
            log.error("Can't get transaction from TM", e);
        }
        log.debugf("Calling next primary key stuf %s", primaryKey);
        ContextEntity contextEntity = em.find(ContextEntity.class, primaryKey);
        if(contextEntity == null) {
            throw new IllegalStateException("Trying to find entity with primary key " + primaryKey + " but such key does not exist.");
        }
        log.infof("onNext entity: %d:%s", contextEntity.getId(), contextEntity.getName());
    }

    @Override
    public void onError(Throwable t) {
        log.error("Error on subscriber happens", t);
    }

    @Override
    public void onComplete() {
        deleteAll();
        log.info("Yay, it's done!");
    }

    private void deleteAll() {
        this.em.createQuery("DELETE from ContextEntity").executeUpdate();
    }
}