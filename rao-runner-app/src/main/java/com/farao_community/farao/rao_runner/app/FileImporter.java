/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.data.refprog.refprog_xml_importer.RefProgImporter;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.app.configuration.MinioAdapter;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.OffsetDateTime;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class FileImporter {

    private final MinioAdapter minioAdapter;

    public FileImporter(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    RaoParameters importRaoParameters(String raoParametersFileUrl) {
        //keep using update method instead of read directly to avoid serialisation issues
        RaoParameters defaultRaoParameters = new RaoParameters();
        InputStream customRaoParameters = minioAdapter.getInputStreamFromUrl(raoParametersFileUrl);
        return JsonRaoParameters.update(defaultRaoParameters, customRaoParameters);
    }

    ZonalData<SensitivityVariableSet> importGlsk(String instant, String glskUrl, Network network) {
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

    Crac importCrac(String cracFileUrl) {
        try {
            return CracImporters.importCrac(minioAdapter.getFileNameFromUrl(cracFileUrl), minioAdapter.getInputStreamFromUrl(cracFileUrl));
        } catch (FaraoException | RaoRunnerException e) {
            throw new RaoRunnerException(String.format("Exception occurred while importing CRAC file: %s. Cause: %s", minioAdapter.getFileNameFromUrl(cracFileUrl), e.getMessage()));
        }
    }

    Network importNetwork(String networkFileUrl) {
        try {
            return Network.read(minioAdapter.getFileNameFromUrl(networkFileUrl), minioAdapter.getInputStreamFromUrl(networkFileUrl));
        } catch (Exception e) {
            throw new RaoRunnerException(String.format("Exception occurred while importing network : %s. Cause: %s ", minioAdapter.getFileNameFromUrl(networkFileUrl), e.getMessage()));
        }
    }
}
