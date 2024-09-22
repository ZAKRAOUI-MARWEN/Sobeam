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
package org.sobeam.server.dao.alarm;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.sobeam.server.cache.CacheSpecsMap;
import org.sobeam.server.cache.RedisTbTransactionalCache;
import org.sobeam.server.cache.TBRedisCacheConfiguration;
import org.sobeam.server.cache.TbTypedJsonRedisSerializer;
import org.sobeam.server.common.data.CacheConstants;
import org.sobeam.server.common.data.EntitySubtype;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.page.PageData;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("AlarmTypesCache")
public class AlarmTypesRedisCache extends RedisTbTransactionalCache<TenantId, PageData<EntitySubtype>> {

    public AlarmTypesRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.ALARM_TYPES_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbTypedJsonRedisSerializer<>(new TypeReference<>() {}));
    }
}
