/**
 * Copyright © 2024 The Sobeam Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sobeam.server.service.stats;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sobeam.server.actors.JsInvokeStats;
import org.sobeam.server.common.stats.StatsCounter;
import org.sobeam.server.common.stats.StatsFactory;
import org.sobeam.server.common.stats.StatsType;

import jakarta.annotation.PostConstruct;

@Service
public class DefaultJsInvokeStats implements JsInvokeStats {
    private static final String REQUESTS = "requests";
    private static final String RESPONSES = "responses";
    private static final String FAILURES = "failures";

    private StatsCounter requestsCounter;
    private StatsCounter responsesCounter;
    private StatsCounter failuresCounter;

    @Autowired
    private StatsFactory statsFactory;

    @PostConstruct
    public void init() {
        String key = StatsType.JS_INVOKE.getName();
        this.requestsCounter = statsFactory.createStatsCounter(key, REQUESTS);
        this.responsesCounter = statsFactory.createStatsCounter(key, RESPONSES);
        this.failuresCounter = statsFactory.createStatsCounter(key, FAILURES);
    }

    @Override
    public void incrementRequests(int amount) {
        requestsCounter.add(amount);
    }

    @Override
    public void incrementResponses(int amount) {
        responsesCounter.add(amount);
    }

    @Override
    public void incrementFailures(int amount) {
        failuresCounter.add(amount);
    }

    @Override
    public int getRequests() {
        return requestsCounter.get();
    }

    @Override
    public int getResponses() {
        return responsesCounter.get();
    }

    @Override
    public int getFailures() {
        return failuresCounter.get();
    }

    @Override
    public void reset() {
        requestsCounter.clear();
        responsesCounter.clear();
        failuresCounter.clear();
    }
}
