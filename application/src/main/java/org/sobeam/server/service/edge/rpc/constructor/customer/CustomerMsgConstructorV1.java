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
package org.sobeam.server.service.edge.rpc.constructor.customer;

import org.springframework.stereotype.Component;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.server.common.data.Customer;
import org.sobeam.server.gen.edge.v1.CustomerUpdateMsg;
import org.sobeam.server.gen.edge.v1.UpdateMsgType;
import org.sobeam.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
public class CustomerMsgConstructorV1 extends BaseCustomerMsgConstructor {

    @Override
    public CustomerUpdateMsg constructCustomerUpdatedMsg(UpdateMsgType msgType, Customer customer) {
        CustomerUpdateMsg.Builder builder = CustomerUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(customer.getId().getId().getMostSignificantBits())
                .setIdLSB(customer.getId().getId().getLeastSignificantBits())
                .setTitle(customer.getTitle());
        if (customer.getCountry() != null) {
            builder.setCountry(customer.getCountry());
        }
        if (customer.getState() != null) {
            builder.setState(customer.getState());
        }
        if (customer.getCity() != null) {
            builder.setCity(customer.getCity());
        }
        if (customer.getAddress() != null) {
            builder.setAddress(customer.getAddress());
        }
        if (customer.getAddress2() != null) {
            builder.setAddress2(customer.getAddress2());
        }
        if (customer.getZip() != null) {
            builder.setZip(customer.getZip());
        }
        if (customer.getPhone() != null) {
            builder.setPhone(customer.getPhone());
        }
        if (customer.getEmail() != null) {
            builder.setEmail(customer.getEmail());
        }
        if (customer.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(customer.getAdditionalInfo()));
        }
        return builder.build();
    }
}
