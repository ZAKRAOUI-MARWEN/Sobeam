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
package org.sobeam.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.sobeam.server.queue.util.AfterStartUp;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@SpringBootConfiguration
@EnableAsync
@EnableScheduling
@ComponentScan({"org.sobeam.server", "org.sobeam.script"})
@Slf4j
public class SobeamServerApplication {

    private static final String SPRING_CONFIG_NAME_KEY = "--spring.config.name";
    private static final String DEFAULT_SPRING_CONFIG_PARAM = SPRING_CONFIG_NAME_KEY + "=" + "sobeam";

    private static long startTs;

    public static void main(String[] args) {
        startTs = System.currentTimeMillis();
        SpringApplication.run(SobeamServerApplication.class, updateArguments(args));
    }

    private static String[] updateArguments(String[] args) {
        if (Arrays.stream(args).noneMatch(arg -> arg.startsWith(SPRING_CONFIG_NAME_KEY))) {
            String[] modifiedArgs = new String[args.length + 1];
            System.arraycopy(args, 0, modifiedArgs, 0, args.length);
            modifiedArgs[args.length] = DEFAULT_SPRING_CONFIG_PARAM;
            return modifiedArgs;
        }
        return args;
    }

    @AfterStartUp(order = Ordered.LOWEST_PRECEDENCE)
    public void afterStartUp() {
        long startupTimeMs = System.currentTimeMillis() - startTs;
        log.info("Started SoBeam in {} seconds", TimeUnit.MILLISECONDS.toSeconds(startupTimeMs));
    }

}
