/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.app.configuration.UrlConfiguration;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.data.refprog.refprogxmlimporter.RefProgImporter;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.powsybl.openrao.virtualhubs.xml.XmlVirtualHubsConfiguration;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.OffsetDateTime;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class FileImporter {

    private final UrlConfiguration urlConfiguration;

    public FileImporter(UrlConfiguration urlConfiguration) {
        this.urlConfiguration = urlConfiguration;
    }

    RaoParameters importRaoParameters(String raoParametersFileUrl) throws FileImporterException {
        try {
            //keep using update method instead of read directly to avoid serialisation issues
            final RaoParameters defaultRaoParameters = new RaoParameters();
            final InputStream customRaoParameters = openUrlStream(raoParametersFileUrl);
            return JsonRaoParameters.update(defaultRaoParameters, customRaoParameters);
        } catch (Exception e) {
            final String message = String.format("Exception occurred while importing rao parameters %s", FilenameUtils.getName(raoParametersFileUrl));
            throw new FileImporterException(message, e);
        }
    }

    public Network importNetwork(final String networkFileUrl) throws FileImporterException {
        try {
            return Network.read(getFileNameFromUrl(networkFileUrl), openUrlStream(networkFileUrl));
        } catch (Exception e) {
            final String message = String.format("Exception occurred while importing network %s", FilenameUtils.getName(networkFileUrl));
            throw new FileImporterException(message, e);
        }
    }

    public Crac importCrac(final String cracFileUrl, final Network network) throws FileImporterException {
        try {
            return Crac.read(getFileNameFromUrl(cracFileUrl), openUrlStream(cracFileUrl), network);
        } catch (Exception e) {
            final String message = String.format("Exception occurred while importing CRAC file %s", FilenameUtils.getName(cracFileUrl));
            throw new FileImporterException(message, e);
        }
    }

    ZonalData<SensitivityVariableSet> importGlsk(final String instant, final String glskUrl, final Network network) throws FileImporterException {
        try {
            final InputStream glskFileInputStream = openUrlStream(glskUrl);
            final GlskDocument ucteGlskProvider = GlskDocumentImporters.importGlsk(glskFileInputStream);
            final OffsetDateTime offsetDateTime = OffsetDateTime.parse(instant);
            return ucteGlskProvider.getZonalGlsks(network, offsetDateTime.toInstant());
        } catch (Exception e) {
            final String message = String.format("Error occurred during GLSK Provider creation for timestamp %s, using GLSK file %s, and CGM network file %s",
                    instant,
                    FilenameUtils.getName(glskUrl),
                    network.getNameOrId());
            throw new FileImporterException(message, e);
        }
    }

    ReferenceProgram importRefProg(final String instant, final String refProgUrl) throws FileImporterException {
        try {
            final InputStream refProgFileInputStream = openUrlStream(refProgUrl);
            final OffsetDateTime offsetDateTime = OffsetDateTime.parse(instant);
            return RefProgImporter.importRefProg(refProgFileInputStream, offsetDateTime);
        } catch (Exception e) {
            final String message = String.format("Error occurred during Reference Program creation for timestamp %s, using refProg file %s",
                    instant,
                    FilenameUtils.getName(refProgUrl));
            throw new FileImporterException(message, e);
        }
    }

    public VirtualHubsConfiguration importVirtualHubs(final String virtualHubsUrl) throws FileImporterException {
        try (InputStream virtualHubsInputStream = openUrlStream(virtualHubsUrl)) {
            return XmlVirtualHubsConfiguration.importConfiguration(virtualHubsInputStream);
        } catch (Exception e) {
            final String message = String.format("Error occurred during virtualhubs Configuration creation using virtualhubs file %s",
                    FilenameUtils.getName(virtualHubsUrl));
            throw new FileImporterException(message, e);
        }
    }

    private InputStream openUrlStream(final String urlString) {
        try {
            if (urlConfiguration.getWhitelist().stream().noneMatch(urlString::startsWith)) {
                throw new RaoRunnerException(String.format("URL '%s' is not part of application's whitelisted url's", urlString));
            }
            final URL url = new URI(urlString).toURL();
            return url.openStream(); // NOSONAR Usage of whitelist not triggered by Sonar quality assessment, even if listed as a solution to the vulnerability
        } catch (IOException | URISyntaxException | IllegalArgumentException e) {
            throw new RaoRunnerException(String.format("Exception occurred while retrieving file content from %s", urlString), e);
        }
    }

    private String getFileNameFromUrl(final String stringUrl) {
        try {
            final URL url = new URI(stringUrl).toURL();
            return FilenameUtils.getName(url.getPath());
        } catch (IOException | URISyntaxException | IllegalArgumentException e) {
            throw new RaoRunnerException(String.format("Exception occurred while retrieving file name from %s", stringUrl), e);
        }
    }
}
