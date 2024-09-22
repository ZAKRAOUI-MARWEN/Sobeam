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
package org.sobeam.server.cache.device;

import com.google.protobuf.InvalidProtocolBufferException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import org.sobeam.server.cache.CacheSpecsMap;
import org.sobeam.server.cache.RedisTbTransactionalCache;
import org.sobeam.server.cache.TBRedisCacheConfiguration;
import org.sobeam.server.cache.TbRedisSerializer;
import org.sobeam.server.common.data.CacheConstants;
import org.sobeam.server.common.data.Device;
import org.sobeam.server.common.util.ProtoUtils;
import org.sobeam.server.gen.transport.TransportProtos;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("DeviceCache")
public class DeviceRedisCache extends RedisTbTransactionalCache<DeviceCacheKey, Device> {

    public DeviceRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.DEVICE_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbRedisSerializer<>() {

            @Override
            public byte[] serialize(Device device) throws SerializationException {
                return ProtoUtils.toProto(device).toByteArray();
            }

            @Override
            public Device deserialize(DeviceCacheKey key, byte[] bytes) throws SerializationException {
                try {
                    return ProtoUtils.fromProto(TransportProtos.DeviceProto.parseFrom(bytes));
                } catch (InvalidProtocolBufferException e) {
                    throw new SerializationException(e.getMessage());
                }
            }
        });
    }
}
