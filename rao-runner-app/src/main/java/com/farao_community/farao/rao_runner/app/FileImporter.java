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

    Optional<ZonalData<LinearGlsk>> importGlsk(RaoRequest raoRequest, Network network) {
        Optional<String> glskFileUrl = raoRequest.getRealGlskFileUrl();
        Optional<String> timestamp = raoRequest.getInstant();
        if (glskFileUrl.isPresent()) {
            try {
                InputStream glskFileInputStream = minioAdapter.getInputStreamFromUrl(glskFileUrl.get());
                GlskDocument ucteGlskProvider = GlskDocumentImporters.importGlsk(glskFileInputStream);
                if (timestamp.isPresent()) {
                    OffsetDateTime offsetDateTime = OffsetDateTime.parse(timestamp.get());
                    return Optional.of(ucteGlskProvider.getZonalGlsks(network, offsetDateTime.toInstant()));
                } else {
                    return Optional.of(ucteGlskProvider.getZonalGlsks(network));
                }
            } catch (Exception e) {
                throw new RaoRunnerException(String.format("Error occurred during GLSK Provider creation for timestamp: %s, using GLSK file: %s, and CGM network file %s. Cause: %s", timestamp, minioAdapter.getFileNameFromUrl(glskFileUrl.get()), network.getNameOrId(), e.getMessage()));
            }
        } else {
            return Optional.empty();
        }
    }

    Optional<ReferenceProgram> importRefProg(RaoRequest raoRequest) {
        Optional<String> refProgFileUrl = raoRequest.getRefprogFileUrl();
        Optional<String> timestamp = raoRequest.getInstant();
        if (refProgFileUrl.isPresent() && timestamp.isPresent()) {
            try {
                InputStream refProgFileInputStream = minioAdapter.getInputStreamFromUrl(refProgFileUrl.get());
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(timestamp.get());
                ReferenceProgram referenceProgram = RefProgImporter.importRefProg(refProgFileInputStream, offsetDateTime);
                return Optional.of(referenceProgram);
            } catch (Exception e) {
                throw new RaoRunnerException(String.format("Error occurred during Reference Program creation for timestamp: %s, using refProg file: %s. Cause: %s", timestamp, minioAdapter.getFileNameFromUrl(refProgFileUrl.get()), e.getMessage()));
            }
        } else {
            return Optional.empty();
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
