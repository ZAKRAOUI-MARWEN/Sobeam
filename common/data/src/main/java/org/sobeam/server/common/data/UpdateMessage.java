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
package org.sobeam.server.common.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Schema
@Data
public class UpdateMessage implements Serializable {

    @Schema(description = "'True' if new platform update is available.")
    private final boolean updateAvailable;
    @Schema(description = "Current SoBeam version.")
    private final String currentVersion;
    @Schema(description = "Latest SoBeam version.")
    private final String latestVersion;
    @Schema(description = "Upgrade instructions URL.")
    private final String upgradeInstructionsUrl;
    @Schema(description = "Current SoBeam version release notes URL.")
    private final String currentVersionReleaseNotesUrl;
    @Schema(description = "Latest SoBeam version release notes URL.")
    private final String latestVersionReleaseNotesUrl;

}
