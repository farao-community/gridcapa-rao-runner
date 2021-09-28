/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app.configuration;

import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@SpringBootTest
class MinioAdapterTest {
    @Autowired
    private MinioAdapter minioAdapter;

    @MockBean
    private MinioClient minioClient;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(minioClient);
    }

    @Test
    void checkuploadFile() throws Exception {
        minioAdapter.uploadFile("file/path", new ByteArrayInputStream("File content".getBytes()));
        Mockito.verify(minioClient, Mockito.times(1)).putObject(Mockito.any());
    }

    @Test
    void checkGetPresignedObjectObject() throws Exception {
        Mockito.when(minioClient.getPresignedObjectUrl(Mockito.any())).thenReturn("http://url");
        String url = minioAdapter.generatePreSignedUrl("file/path");
        Mockito.verify(minioClient, Mockito.times(1)).getPresignedObjectUrl(Mockito.any());
        assertEquals("http://url", url);
    }

    @Test
    void checkFileNameReturnedCorrectlyFromUrl() {
        String stringUrl = "http://localhost:9000/folder/id/fileName.xml?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minioadmin-Amz-SignedHeaders=host&X-Amz-Signature=61e252359e5cb";
        assertEquals("fileName.xml", minioAdapter.getFileNameFromUrl(stringUrl));
    }

    @Test
    void checkExceptionThrown() {
        UrlWhitelistConfiguration urlWhitelistConfigurationMock = Mockito.mock(UrlWhitelistConfiguration.class);
        Mockito.when(urlWhitelistConfigurationMock.getWhitelist()).thenReturn(Arrays.asList("url1", "url2"));
        Exception exception = assertThrows(RaoRunnerException.class, () -> minioAdapter.getInputStreamFromUrl("notWhiteListedUrl"));
        String expectedMessage = "is not part of application's whitelisted url's";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

}
