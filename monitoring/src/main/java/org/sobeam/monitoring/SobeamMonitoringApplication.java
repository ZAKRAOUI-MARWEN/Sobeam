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
package org.sobeam.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.sobeam.common.util.SoBeamThreadFactory;
import org.sobeam.monitoring.service.BaseMonitoringService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class SobeamMonitoringApplication {

    @Autowired
    private List<BaseMonitoringService<?, ?>> monitoringServices;

    @Value("${monitoring.monitoring_rate_ms}")
    private int monitoringRateMs;

    public static void main(String[] args) {
        new SpringApplicationBuilder(SobeamMonitoringApplication.class)
                .properties(Map.of("spring.config.name", "tb-monitoring"))
                .run(args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startMonitoring() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(SoBeamThreadFactory.forName("monitoring-executor"));
        scheduler.scheduleWithFixedDelay(() -> {
            monitoringServices.forEach(monitoringService -> {
                monitoringService.runChecks();
            });
        }, 0, monitoringRateMs, TimeUnit.MILLISECONDS);
    }

}
