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
package org.sobeam.server.service.edge.rpc.constructor.ota;

import com.google.protobuf.ByteString;
import org.springframework.stereotype.Component;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.server.common.data.OtaPackage;
import org.sobeam.server.gen.edge.v1.OtaPackageUpdateMsg;
import org.sobeam.server.gen.edge.v1.UpdateMsgType;
import org.sobeam.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
public class OtaPackageMsgConstructorV1 extends BaseOtaPackageMsgConstructor {

    @Override
    public OtaPackageUpdateMsg constructOtaPackageUpdatedMsg(UpdateMsgType msgType, OtaPackage otaPackage) {
        OtaPackageUpdateMsg.Builder builder = OtaPackageUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(otaPackage.getId().getId().getMostSignificantBits())
                .setIdLSB(otaPackage.getId().getId().getLeastSignificantBits())
                .setType(otaPackage.getType().name())
                .setTitle(otaPackage.getTitle())
                .setVersion(otaPackage.getVersion())
                .setTag(otaPackage.getTag());

        if (otaPackage.getDeviceProfileId() != null) {
            builder.setDeviceProfileIdMSB(otaPackage.getDeviceProfileId().getId().getMostSignificantBits())
                    .setDeviceProfileIdLSB(otaPackage.getDeviceProfileId().getId().getLeastSignificantBits());
        }

        if (otaPackage.getUrl() != null) {
            builder.setUrl(otaPackage.getUrl());
        }
        if (otaPackage.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(otaPackage.getAdditionalInfo()));
        }
        if (otaPackage.getFileName() != null) {
            builder.setFileName(otaPackage.getFileName());
        }
        if (otaPackage.getContentType() != null) {
            builder.setContentType(otaPackage.getContentType());
        }
        if (otaPackage.getChecksumAlgorithm() != null) {
            builder.setChecksumAlgorithm(otaPackage.getChecksumAlgorithm().name());
        }
        if (otaPackage.getChecksum() != null) {
            builder.setChecksum(otaPackage.getChecksum());
        }
        if (otaPackage.getDataSize() != null) {
            builder.setDataSize(otaPackage.getDataSize());
        }
        if (otaPackage.getData() != null) {
            builder.setData(ByteString.copyFrom(otaPackage.getData().array()));
        }
        return builder.build();
    }
}
