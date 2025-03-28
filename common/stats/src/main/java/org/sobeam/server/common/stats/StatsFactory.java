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
package org.sobeam.server.common.stats;

import io.micrometer.core.instrument.Timer;

public interface StatsFactory {

    StatsCounter createStatsCounter(String key, String statsName, String... otherTags);

    DefaultCounter createDefaultCounter(String key, String... tags);

    <T extends Number> T createGauge(String key, T number, String... tags);

    MessagesStats createMessagesStats(String key);

    Timer createTimer(String key, String... tags);

    StatsTimer createTimer(StatsType type, String name, String... tags);

}
