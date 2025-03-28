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
package org.sobeam.server.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.sobeam.server.common.data.ImageDescriptor;
import org.sobeam.server.common.data.ImageExportData;
import org.sobeam.server.common.data.ResourceType;
import org.sobeam.server.common.data.TbImageDeleteResult;
import org.sobeam.server.common.data.TbResource;
import org.sobeam.server.common.data.TbResourceInfo;
import org.sobeam.server.common.data.exception.SobeamException;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.common.data.security.Authority;
import org.sobeam.server.common.data.util.ThrowingSupplier;
import org.sobeam.server.dao.resource.ImageCacheKey;
import org.sobeam.server.dao.resource.ImageService;
import org.sobeam.server.dao.service.validator.ResourceDataValidator;
import org.sobeam.server.queue.util.TbCoreComponent;
import org.sobeam.server.service.resource.TbImageService;
import org.sobeam.server.service.security.model.SecurityUser;
import org.sobeam.server.service.security.permission.Operation;
import org.sobeam.server.service.security.permission.Resource;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static org.sobeam.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.sobeam.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.sobeam.server.controller.ControllerConstants.RESOURCE_INCLUDE_SYSTEM_IMAGES_DESCRIPTION;
import static org.sobeam.server.controller.ControllerConstants.RESOURCE_TEXT_SEARCH_DESCRIPTION;
import static org.sobeam.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.sobeam.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;

@Slf4j
@RestController
@TbCoreComponent
@RequiredArgsConstructor
public class ImageController extends BaseController {

    private final ImageService imageService;
    private final TbImageService tbImageService;
    private final ResourceDataValidator resourceValidator;

    @Value("${cache.image.systemImagesBrowserTtlInMinutes:0}")
    private int systemImagesBrowserTtlInMinutes;
    @Value("${cache.image.tenantImagesBrowserTtlInMinutes:0}")
    private int tenantImagesBrowserTtlInMinutes;

    private static final String IMAGE_URL = "/api/images/{type}/{key}";
    private static final String SYSTEM_IMAGE = "system";
    private static final String TENANT_IMAGE = "tenant";

    private static final String IMAGE_TYPE_PARAM_DESCRIPTION = "Type of the image: tenant or system";
    private static final String IMAGE_TYPE_PARAM_ALLOWABLE_VALUES = "tenant, system";
    private static final String IMAGE_KEY_PARAM_DESCRIPTION = "Image resource key, for example thermostats_dashboard_background.jpeg";

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PostMapping(value = "/api/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TbResourceInfo uploadImage(@RequestPart MultipartFile file,
                                      @RequestPart(required = false) String title) throws Exception {
        SecurityUser user = getCurrentUser();
        TbResource image = new TbResource();
        image.setTenantId(user.getTenantId());
        accessControlService.checkPermission(user, Resource.TB_RESOURCE, Operation.CREATE, null, image);
        resourceValidator.validateResourceSize(user.getTenantId(), null, file.getSize());

        image.setFileName(file.getOriginalFilename());
        if (StringUtils.isNotEmpty(title)) {
            image.setTitle(title);
        } else {
            image.setTitle(file.getOriginalFilename());
        }
        image.setResourceType(ResourceType.IMAGE);
        ImageDescriptor descriptor = new ImageDescriptor();
        descriptor.setMediaType(file.getContentType());
        image.setDescriptorValue(descriptor);
        image.setData(file.getBytes());
        image.setPublic(true);
        return tbImageService.save(image, user);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PutMapping(value = IMAGE_URL, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TbResourceInfo updateImage(@Parameter(description = IMAGE_TYPE_PARAM_DESCRIPTION, schema = @Schema(allowableValues = {"tenant", "system"}), required = true)
                                      @PathVariable String type,
                                      @Parameter(description = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                      @PathVariable String key,
                                      @RequestPart MultipartFile file) throws Exception {
        TbResourceInfo imageInfo = checkImageInfo(type, key, Operation.WRITE);
        resourceValidator.validateResourceSize(getTenantId(), imageInfo.getId(), file.getSize());

        TbResource image = new TbResource(imageInfo);
        image.setData(file.getBytes());
        image.setFileName(file.getOriginalFilename());
        image.updateDescriptor(ImageDescriptor.class, descriptor -> {
            descriptor.setMediaType(file.getContentType());
            return descriptor;
        });
        return tbImageService.save(image, getCurrentUser());
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PutMapping(IMAGE_URL + "/info")
    public TbResourceInfo updateImageInfo(@Parameter(description = IMAGE_TYPE_PARAM_DESCRIPTION, schema = @Schema(allowableValues = {"tenant", "system"}), required = true)
                                          @PathVariable String type,
                                          @Parameter(description = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                          @PathVariable String key,
                                          @RequestBody TbResourceInfo request) throws SobeamException {
        TbResourceInfo imageInfo = checkImageInfo(type, key, Operation.WRITE);
        TbResourceInfo newImageInfo = new TbResourceInfo(imageInfo);
        newImageInfo.setTitle(request.getTitle());
        return tbImageService.save(newImageInfo, imageInfo, getCurrentUser());
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PutMapping(IMAGE_URL + "/public/{isPublic}")
    public TbResourceInfo updateImagePublicStatus(@Parameter(description = IMAGE_TYPE_PARAM_DESCRIPTION, schema = @Schema(allowableValues = {"tenant", "system"}), required = true)
                                                  @PathVariable String type,
                                                  @Parameter(description = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                                  @PathVariable String key,
                                                  @PathVariable boolean isPublic) throws SobeamException {
        TbResourceInfo imageInfo = checkImageInfo(type, key, Operation.WRITE);
        TbResourceInfo newImageInfo = new TbResourceInfo(imageInfo);
        newImageInfo.setPublic(isPublic);
        return tbImageService.save(newImageInfo, imageInfo, getCurrentUser());
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = IMAGE_URL, produces = "image/*")
    public ResponseEntity<ByteArrayResource> downloadImage(@Parameter(description = IMAGE_TYPE_PARAM_DESCRIPTION, schema = @Schema(allowableValues = {"tenant", "system"}), required = true)
                                                           @PathVariable String type,
                                                           @Parameter(description = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                                           @PathVariable String key,
                                                           @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag) throws Exception {
        return downloadIfChanged(type, key, etag, false);
    }

    @GetMapping(value = "/api/images/public/{publicResourceKey}", produces = "image/*")
    public ResponseEntity<ByteArrayResource> downloadPublicImage(@PathVariable String publicResourceKey,
                                                                 @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag) throws Exception {
        ImageCacheKey cacheKey = ImageCacheKey.forPublicImage(publicResourceKey);
        return downloadIfChanged(cacheKey, etag, () -> imageService.getPublicImageInfoByKey(publicResourceKey));
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(value = IMAGE_URL + "/export")
    public ImageExportData exportImage(@Parameter(description = IMAGE_TYPE_PARAM_DESCRIPTION, schema = @Schema(allowableValues = {"tenant", "system"}), required = true)
                                       @PathVariable String type,
                                       @Parameter(description = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                       @PathVariable String key) throws Exception {
        TbResourceInfo imageInfo = checkImageInfo(type, key, Operation.READ);
        ImageDescriptor descriptor = imageInfo.getDescriptor(ImageDescriptor.class);
        byte[] data = imageService.getImageData(imageInfo.getTenantId(), imageInfo.getId());
        return ImageExportData.builder()
                .mediaType(descriptor.getMediaType())
                .fileName(imageInfo.getFileName())
                .title(imageInfo.getTitle())
                .resourceKey(imageInfo.getResourceKey())
                .isPublic(imageInfo.isPublic())
                .publicResourceKey(imageInfo.getPublicResourceKey())
                .data(Base64.getEncoder().encodeToString(data))
                .build();
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @PutMapping("/api/image/import")
    public TbResourceInfo importImage(@RequestBody ImageExportData imageData) throws Exception {
        SecurityUser user = getCurrentUser();
        TbResource image = new TbResource();
        image.setTenantId(user.getTenantId());
        accessControlService.checkPermission(user, Resource.TB_RESOURCE, Operation.CREATE, null, image);

        image.setFileName(imageData.getFileName());
        if (StringUtils.isNotEmpty(imageData.getTitle())) {
            image.setTitle(imageData.getTitle());
        } else {
            image.setTitle(imageData.getFileName());
        }
        image.setResourceType(ResourceType.IMAGE);
        image.setResourceKey(imageData.getResourceKey());
        image.setPublic(imageData.isPublic());
        image.setPublicResourceKey(imageData.getPublicResourceKey());
        ImageDescriptor descriptor = new ImageDescriptor();
        descriptor.setMediaType(imageData.getMediaType());
        image.setDescriptorValue(descriptor);
        image.setData(Base64.getDecoder().decode(imageData.getData()));
        return tbImageService.save(image, user);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = IMAGE_URL + "/preview", produces = "image/png")
    public ResponseEntity<ByteArrayResource> downloadImagePreview(@Parameter(description = IMAGE_TYPE_PARAM_DESCRIPTION, schema = @Schema(allowableValues = {"tenant", "system"}), required = true)
                                                                  @PathVariable String type,
                                                                  @Parameter(description = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                                                  @PathVariable String key,
                                                                  @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String etag) throws Exception {
        return downloadIfChanged(type, key, etag, true);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping(IMAGE_URL + "/info")
    public TbResourceInfo getImageInfo(@Parameter(description = IMAGE_TYPE_PARAM_DESCRIPTION, schema = @Schema(allowableValues = {"tenant", "system"}), required = true)
                                       @PathVariable String type,
                                       @Parameter(description = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                       @PathVariable String key) throws SobeamException {
        return checkImageInfo(type, key, Operation.READ);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @GetMapping("/api/images")
    public PageData<TbResourceInfo> getImages(@Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
                                              @RequestParam int pageSize,
                                              @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
                                              @RequestParam int page,
                                              @Parameter(description = RESOURCE_INCLUDE_SYSTEM_IMAGES_DESCRIPTION)
                                              @RequestParam(required = false) boolean includeSystemImages,
                                              @Parameter(description = RESOURCE_TEXT_SEARCH_DESCRIPTION)
                                              @RequestParam(required = false) String textSearch,
                                              @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "title", "resourceType", "tenantId"}))
                                              @RequestParam(required = false) String sortProperty,
                                              @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
                                              @RequestParam(required = false) String sortOrder) throws SobeamException {
        // PE: generic permission
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        TenantId tenantId = getTenantId();
        if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN || !includeSystemImages) {
            return checkNotNull(imageService.getImagesByTenantId(tenantId, pageLink));
        } else {
            return checkNotNull(imageService.getAllImagesByTenantId(tenantId, pageLink));
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @DeleteMapping(IMAGE_URL)
    public ResponseEntity<TbImageDeleteResult> deleteImage(@Parameter(description = IMAGE_TYPE_PARAM_DESCRIPTION, schema = @Schema(allowableValues = {"tenant", "system"}), required = true)
                                                           @PathVariable String type,
                                                           @Parameter(description = IMAGE_KEY_PARAM_DESCRIPTION, required = true)
                                                           @PathVariable String key,
                                                           @RequestParam(name = "force", required = false) boolean force) throws SobeamException {
        TbResourceInfo imageInfo = checkImageInfo(type, key, Operation.DELETE);
        TbImageDeleteResult result = tbImageService.delete(imageInfo, getCurrentUser(), force);
        return (result.isSuccess() ? ResponseEntity.ok() : ResponseEntity.badRequest()).body(result);
    }

    private ResponseEntity<ByteArrayResource> downloadIfChanged(String type, String key, String etag, boolean preview) throws Exception {
        ImageCacheKey cacheKey = ImageCacheKey.forImage(getTenantId(type), key, preview);
        return downloadIfChanged(cacheKey, etag, () -> checkImageInfo(type, key, Operation.READ));
    }

    private ResponseEntity<ByteArrayResource> downloadIfChanged(ImageCacheKey cacheKey, String etag, ThrowingSupplier<TbResourceInfo> imageInfoSupplier) throws Exception {
        if (StringUtils.isNotEmpty(etag)) {
            etag = StringUtils.remove(etag, '\"'); // etag is wrapped in double quotes due to HTTP specification
            if (etag.equals(tbImageService.getETag(cacheKey))) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }
        }

        TbResourceInfo imageInfo = checkNotNull(imageInfoSupplier.get());
        String fileName = imageInfo.getFileName();
        ImageDescriptor descriptor = imageInfo.getDescriptor(ImageDescriptor.class);
        byte[] data;
        if (cacheKey.isPreview()) {
            descriptor = descriptor.getPreviewDescriptor();
            data = imageService.getImagePreview(imageInfo.getTenantId(), imageInfo.getId());
        } else {
            data = imageService.getImageData(imageInfo.getTenantId(), imageInfo.getId());
        }
        tbImageService.putETag(cacheKey, descriptor.getEtag());
        var result = ResponseEntity.ok()
                .header("Content-Type", descriptor.getMediaType())
                .contentLength(data.length)
                .eTag(descriptor.getEtag());
        if (!cacheKey.isPublic()) {
            result
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName)
                    .header("x-filename", fileName);
        }
        if (systemImagesBrowserTtlInMinutes > 0 && imageInfo.getTenantId().isSysTenantId()) {
            result.cacheControl(CacheControl.maxAge(systemImagesBrowserTtlInMinutes, TimeUnit.MINUTES));
        } else if (tenantImagesBrowserTtlInMinutes > 0 && !imageInfo.getTenantId().isSysTenantId()) {
            result.cacheControl(CacheControl.maxAge(tenantImagesBrowserTtlInMinutes, TimeUnit.MINUTES));
        } else {
            result.cacheControl(CacheControl.noCache());
        }
        return result.body(new ByteArrayResource(data));
    }

    private TbResourceInfo checkImageInfo(String imageType, String key, Operation operation) throws SobeamException {
        TenantId tenantId = getTenantId(imageType);
        TbResourceInfo imageInfo = imageService.getImageInfoByTenantIdAndKey(tenantId, key);
        checkEntity(getCurrentUser(), checkNotNull(imageInfo), operation);
        return imageInfo;
    }

    private TenantId getTenantId(String imageType) throws SobeamException {
        TenantId tenantId;
        if (imageType.equals(TENANT_IMAGE)) {
            tenantId = getTenantId();
        } else if (imageType.equals(SYSTEM_IMAGE)) {
            tenantId = TenantId.SYS_TENANT_ID;
        } else {
            throw new IllegalArgumentException("Invalid image URL");
        }
        return tenantId;
    }

}