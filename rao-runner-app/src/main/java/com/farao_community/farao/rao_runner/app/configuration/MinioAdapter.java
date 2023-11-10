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
    private final Logger businessLogger;
    private final String minioBucket;
    private final String basePath;

    private static final Logger LOGGER = LoggerFactory.getLogger(MinioAdapter.class);

    public MinioAdapter(MinioConfiguration minioConfiguration, MinioClient minioClient, UrlWhitelistConfiguration urlWhitelistConfiguration, Logger businessLogger) {
        this.minioClient = minioClient;
        this.minioBucket = minioConfiguration.getBucket();
        this.basePath = minioConfiguration.getBasePath();
        this.urlWhitelistConfiguration = urlWhitelistConfiguration;
        this.businessLogger = businessLogger;
    }

    public void uploadFile(String pathDestination, InputStream sourceInputStream) {
        try {
            createBucketIfDoesNotExist();
            minioClient.putObject(PutObjectArgs.builder().bucket(minioBucket).object(pathDestination).stream(sourceInputStream, -1, 50000000).build());
        } catch (Exception e) {
            String message = String.format("Exception occurred while uploading file \"%s\" to minio server", pathDestination);
            LOGGER.error(message);
            throw new RaoRunnerException(message, e);
        }
    }

    public String generatePreSignedUrl(String minioPath) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().bucket(minioBucket).object(minioPath).expiry(DEFAULT_DOWNLOAD_LINK_EXPIRY_IN_DAYS, TimeUnit.DAYS).method(Method.GET).build());
        } catch (Exception e) {
            String message = "Exception in MinIO connection";
            LOGGER.error(message);
            throw new RaoRunnerException(message, e);
        }
    }

    private void createBucketIfDoesNotExist() {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioBucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioBucket).build());
            }
        } catch (Exception e) {
            String message = String.format("Exception occurred while creating bucket %s", minioBucket);
            LOGGER.error(message);
            throw new RaoRunnerException(message, e);
        }
    }

    public String getDefaultBasePath() {
        return basePath;
    }

    public InputStream getInputStreamFromUrl(String url) {
        try {
            if (urlWhitelistConfiguration.getWhitelist().stream().noneMatch(url::startsWith)) {
                String message = String.format("URL '%s' is not part of application's whitelisted url's.", url);
                LOGGER.error(message);
                throw new RaoRunnerException(message);
            }
            return new URL(url).openStream();
        } catch (IOException e) {
            businessLogger.error("Error while retrieving content of file \"{}\", link may have expired.", getFileNameFromUrl(url));
            throw new RaoRunnerException(String.format("Exception occurred while retrieving file content from %s", url), e);
        }
    }

    public String getFileNameFromUrl(String stringUrl) {
        try {
            URL url = new URL(stringUrl);
            return FilenameUtils.getName(url.getPath());
        } catch (IOException e) {
            String message = String.format("Exception occurred while retrieving file name from %s", stringUrl);
            LOGGER.error(message);
            throw new RaoRunnerException(message, e);
        }
    }
}
