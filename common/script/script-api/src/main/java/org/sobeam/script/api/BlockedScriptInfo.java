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
package org.sobeam.script.api;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BlockedScriptInfo {
    private final long maxScriptBlockDurationMs;
    private final AtomicInteger counter;
    private long expirationTime;

    BlockedScriptInfo(int maxScriptBlockDuration) {
        this.maxScriptBlockDurationMs = TimeUnit.SECONDS.toMillis(maxScriptBlockDuration);
        this.counter = new AtomicInteger(0);
    }

    public int get() {
        return counter.get();
    }

    public int incrementAndGet() {
        int result = counter.incrementAndGet();
        expirationTime = System.currentTimeMillis() + maxScriptBlockDurationMs;
        return result;
    }

    public long getExpirationTime() {
        return expirationTime;
    }
}