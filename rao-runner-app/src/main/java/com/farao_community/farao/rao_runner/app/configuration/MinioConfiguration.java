/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app.configuration;

import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Configuration
public class MinioConfiguration {

    @Value("${minio.access.name}")
    private String accessKey;
    @Value("${minio.access.secret}")
    private String accessSecret;
    @Value("${minio.url}")
    private String minioUrl;
    @Value("${minio.bucket}")
    private String bucket;
    @Value("${minio.base-path}")
    private String basePath;

    @Bean
    public MinioClient generateMinioClient() {
        try {
            return MinioClient.builder().endpoint(minioUrl).credentials(accessKey, accessSecret).build();
        } catch (Exception e) {
            throw new RaoRunnerException("Exception in MinIO client", e);
        }
    }

    public String getBucket() {
        return bucket;
    }

    public String getBasePath() {
        return basePath;
    }
}

