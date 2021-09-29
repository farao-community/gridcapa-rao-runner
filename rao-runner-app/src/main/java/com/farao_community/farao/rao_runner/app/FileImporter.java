/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.io.GlskDocumentImporters;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.data.refprog.refprog_xml_importer.RefProgImporter;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.app.configuration.MinioAdapter;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class FileImporter {

    private final MinioAdapter minioAdapter;

    public FileImporter(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    RaoParameters importRaoParameters(RaoRequest raoRequest) {
        RaoParameters defaultRaoParameters = RaoParameters.load();
        Optional<String> raoParametersFileUrl = raoRequest.getRaoParametersFileUrl();
        if (raoParametersFileUrl.isPresent()) {
            InputStream jsonRaoParametersInputStream = minioAdapter.getInputStreamFromUrl(raoParametersFileUrl.get());
            return JsonRaoParameters.update(defaultRaoParameters, jsonRaoParametersInputStream);
        } else {
            return defaultRaoParameters;
        }
    }

    ZonalData<LinearGlsk> importGlsk(String instant, String glskUrl, Network network) {
        try {
            InputStream glskFileInputStream = minioAdapter.getInputStreamFromUrl(glskUrl);
            GlskDocument ucteGlskProvider = GlskDocumentImporters.importGlsk(glskFileInputStream);
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(instant);
            return ucteGlskProvider.getZonalGlsks(network, offsetDateTime.toInstant());
        } catch (Exception e) {
            throw new RaoRunnerException(
                    String.format("Error occurred during GLSK Provider creation for timestamp: %s, using GLSK file: %s, and CGM network file %s. Cause: %s",
                            instant,
                            minioAdapter.getFileNameFromUrl(glskUrl),
                            network.getNameOrId(),
                            e.getMessage()));
        }
    }

    ReferenceProgram importRefProg(String instant, String refProgUrl) {
        try {
            InputStream refProgFileInputStream = minioAdapter.getInputStreamFromUrl(refProgUrl);
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(instant);
            return RefProgImporter.importRefProg(refProgFileInputStream, offsetDateTime);
        } catch (Exception e) {
            throw new RaoRunnerException(
                    String.format("Error occurred during Reference Program creation for timestamp: %s, using refProg file: %s. Cause: %s",
                            instant,
                            minioAdapter.getFileNameFromUrl(refProgUrl),
                            e.getMessage()));
        }
    }

    Crac importCrac(RaoRequest raoRequest) {
        String cracFileUrl = raoRequest.getCracFileUrl();
        try {
            return CracImporters.importCrac(minioAdapter.getFileNameFromUrl(cracFileUrl), minioAdapter.getInputStreamFromUrl(cracFileUrl));
        } catch (FaraoException | RaoRunnerException e) {
            throw new RaoRunnerException(String.format("Exception occurred while importing CRAC file: %s. Cause: %s", minioAdapter.getFileNameFromUrl(cracFileUrl), e.getMessage()));
        }
    }

    Network importNetwork(RaoRequest raoRequest) {
        String networkFileUrl = raoRequest.getNetworkFileUrl();
        try {
            return Importers.loadNetwork(minioAdapter.getFileNameFromUrl(networkFileUrl), minioAdapter.getInputStreamFromUrl(networkFileUrl));
        } catch (Exception e) {
            throw new RaoRunnerException(String.format("Exception occurred while importing network : %s. Cause: %s ", minioAdapter.getFileNameFromUrl(networkFileUrl), e.getMessage()));
        }
    }
}
