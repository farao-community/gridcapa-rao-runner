/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app.configuration;

import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import io.minio.*;
import io.minio.http.Method;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Component
public class MinioAdapter {
    private static final int DEFAULT_DOWNLOAD_LINK_EXPIRY_IN_DAYS = 7;

    private final MinioClient minioClient;
    private final UrlWhitelistConfiguration urlWhitelistConfiguration;
    private final String minioBucket;
    private final String basePath;

    private static final Logger LOGGER = LoggerFactory.getLogger(MinioAdapter.class);

    public MinioAdapter(MinioConfiguration minioConfiguration, MinioClient minioClient, UrlWhitelistConfiguration urlWhitelistConfiguration) {
        this.minioClient = minioClient;
        this.minioBucket = minioConfiguration.getBucket();
        this.basePath = minioConfiguration.getBasePath();
        this.urlWhitelistConfiguration = urlWhitelistConfiguration;
    }

    public void uploadFile(String pathDestination, InputStream sourceInputStream) {
        try {
            createBucketIfDoesNotExist();
            minioClient.putObject(PutObjectArgs.builder().bucket(minioBucket).object(pathDestination).stream(sourceInputStream, -1, 50000000).build());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new RaoRunnerException(String.format("Exception occurred while uploading file: %s, to minio server", pathDestination));
        }
    }

    public String generatePreSignedUrl(String minioPath) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().bucket(minioBucket).object(minioPath).expiry(DEFAULT_DOWNLOAD_LINK_EXPIRY_IN_DAYS, TimeUnit.DAYS).method(Method.GET).build());
        } catch (Exception e) {
            throw new RaoRunnerException("Exception in MinIO connection.", e);
        }
    }

    private void createBucketIfDoesNotExist() {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioBucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioBucket).build());
            }
        } catch (Exception e) {
            LOGGER.error(String.format("Exception occurred while creating bucket: %s", minioBucket));
            throw new RaoRunnerException(String.format("Exception occurred while creating bucket: %s", minioBucket));
        }
    }

    public String getDefaultBasePath() {
        return basePath;
    }

    public InputStream getInputStreamFromUrl(String url) {
        try {
            if (urlWhitelistConfiguration.getWhitelist().stream().noneMatch(url::startsWith)) {
                throw new RaoRunnerException(String.format("URL '%s' is not part of application's whitelisted url's.", url));
            }
            return new URL(url).openStream();
        } catch (IOException e) {
            throw new RaoRunnerException(String.format("Exception occurred while retrieving file content from : %s Cause: %s ", url, e.getMessage()));
        }
    }

    public String getFileNameFromUrl(String stringUrl) {
        try {
            URL url = new URL(stringUrl);
            return FilenameUtils.getName(url.getPath());
        } catch (IOException e) {
            throw new RaoRunnerException(String.format("Exception occurred while retrieving file name from : %s Cause: %s ", stringUrl, e.getMessage()));
        }
    }
}
