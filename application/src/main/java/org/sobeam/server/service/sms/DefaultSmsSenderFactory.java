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
package org.sobeam.server.service.sms;

import org.springframework.stereotype.Component;
import org.sobeam.rule.engine.api.sms.SmsSender;
import org.sobeam.rule.engine.api.sms.SmsSenderFactory;
import org.sobeam.server.common.data.sms.config.AwsSnsSmsProviderConfiguration;
import org.sobeam.server.common.data.sms.config.SmppSmsProviderConfiguration;
import org.sobeam.server.common.data.sms.config.SmsProviderConfiguration;
import org.sobeam.server.common.data.sms.config.TwilioSmsProviderConfiguration;
import org.sobeam.server.service.sms.aws.AwsSmsSender;
import org.sobeam.server.service.sms.smpp.SmppSmsSender;
import org.sobeam.server.service.sms.twilio.TwilioSmsSender;

@Component
public class DefaultSmsSenderFactory implements SmsSenderFactory {

    @Override
    public SmsSender createSmsSender(SmsProviderConfiguration config) {
        switch (config.getType()) {
            case AWS_SNS:
                return new AwsSmsSender((AwsSnsSmsProviderConfiguration)config);
            case TWILIO:
                return new TwilioSmsSender((TwilioSmsProviderConfiguration)config);
            case SMPP:
                return new SmppSmsSender((SmppSmsProviderConfiguration) config);
            default:
                throw new RuntimeException("Unknown SMS provider type " + config.getType());
        }
    }

}
