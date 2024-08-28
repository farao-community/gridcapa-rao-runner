/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.data.refprog.refprogxmlimporter.RefProgImporter;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.app.configuration.UrlConfiguration;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.powsybl.openrao.virtualhubs.xml.XmlVirtualHubsConfiguration;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
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

    RaoParameters importRaoParameters(String raoParametersFileUrl) {
        //keep using update method instead of read directly to avoid serialisation issues
        RaoParameters defaultRaoParameters = new RaoParameters();
        InputStream customRaoParameters = openUrlStream(raoParametersFileUrl);
        return JsonRaoParameters.update(defaultRaoParameters, customRaoParameters);
    }

    ZonalData<SensitivityVariableSet> importGlsk(String instant, String glskUrl, Network network) {
        try {
            InputStream glskFileInputStream = openUrlStream(glskUrl);
            GlskDocument ucteGlskProvider = GlskDocumentImporters.importGlsk(glskFileInputStream);
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(instant);
            return ucteGlskProvider.getZonalGlsks(network, offsetDateTime.toInstant());
        } catch (Exception e) {
            String message = String.format("Error occurred during GLSK Provider creation for timestamp %s, using GLSK file %s, and CGM network file %s",
                instant,
                getFileNameFromUrl(glskUrl),
                network.getNameOrId());
            throw new RaoRunnerException(message, e);
        }
    }

    ReferenceProgram importRefProg(String instant, String refProgUrl) {
        try {
            InputStream refProgFileInputStream = openUrlStream(refProgUrl);
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(instant);
            return RefProgImporter.importRefProg(refProgFileInputStream, offsetDateTime);
        } catch (Exception e) {
            String message = String.format("Error occurred during Reference Program creation for timestamp %s, using refProg file %s",
                instant,
                getFileNameFromUrl(refProgUrl));
            throw new RaoRunnerException(message, e);
        }
    }

    public VirtualHubsConfiguration importVirtualHubs(String virtualHubsUrl) {
        try (InputStream virtualHubsInputStream = openUrlStream(virtualHubsUrl)) {
            return XmlVirtualHubsConfiguration.importConfiguration(virtualHubsInputStream);
        } catch (Exception e) {
            String message = String.format("Error occurred during virtualhubs Configuration creation using virtualhubs file %s",
                    getFileNameFromUrl(virtualHubsUrl));
            throw new RaoRunnerException(message, e);
        }
    }

    Crac importCrac(String cracFileUrl, Network network) {
        try {
            return Crac.read(getFileNameFromUrl(cracFileUrl), openUrlStream(cracFileUrl), network);
        } catch (OpenRaoException | RaoRunnerException | IOException e) {
            String message = String.format("Exception occurred while importing CRAC file %s", getFileNameFromUrl(cracFileUrl));
            throw new RaoRunnerException(message, e);
        }
    }

    Network importNetwork(String networkFileUrl) {
        try {
            return Network.read(getFileNameFromUrl(networkFileUrl), openUrlStream(networkFileUrl));
        } catch (Exception e) {
            String message = String.format("Exception occurred while importing network %s", getFileNameFromUrl(networkFileUrl));
            throw new RaoRunnerException(message, e);
        }
    }

    private InputStream openUrlStream(String urlString) {
        try {
            if (urlConfiguration.getWhitelist().stream().noneMatch(urlString::startsWith)) {
                throw new RaoRunnerException(String.format("URL '%s' is not part of application's whitelisted url's.", urlString));
            }
            URL url = new URL(urlString);
            return url.openStream(); // NOSONAR Usage of whitelist not triggered by Sonar quality assessment, even if listed as a solution to the vulnerability
        } catch (IOException e) {
            throw new RaoRunnerException(String.format("Exception occurred while retrieving file content from : %s", urlString), e);
        }
    }

    private String getFileNameFromUrl(String stringUrl) {
        try {
            URL url = new URL(stringUrl);
            return FilenameUtils.getName(url.getPath());
        } catch (IOException e) {
            throw new RaoRunnerException(String.format("Exception occurred while retrieving file name from : %s", stringUrl), e);
        }
    }
}
