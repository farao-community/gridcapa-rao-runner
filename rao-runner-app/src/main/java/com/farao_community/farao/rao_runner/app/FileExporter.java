package com.farao_community.farao.rao_runner.app;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultExporter;
import com.farao_community.farao.rao_runner.api.exceptions.RaoRunnerException;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.app.configuration.MinioAdapter;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Objects;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class FileExporter {
    private static final String NETWORK = "networkWithPRA.xiidm";
    private static final String RAO_RESULT = "raoResult.json";
    private static final String IIDM_EXPORT_FORMAT = "XIIDM";
    private static final String IIDM_EXTENSION = "xiidm";

    private final MinioAdapter minioAdapter;

    public FileExporter(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    String generateResultsDestination(RaoRequest raoRequest) {
        return raoRequest.getResultsDestination().orElse(minioAdapter.getDefaultBasePath() + "/" + raoRequest.getId());
    }

    String exportAndSaveNetworkWithPra(RaoResult raoResult, Network network, String resultsDestination) {
        String networkWithPraFileUrl;
        if (Objects.nonNull(raoResult) && raoResult.getComputationStatus() != ComputationStatus.FAILURE) {
            MemDataSource dataSource = new MemDataSource();
            Exporters.export(IIDM_EXPORT_FORMAT, network, null, dataSource);
            String networkWithPRADestinationPath = resultsDestination + File.separator + NETWORK;
            minioAdapter.uploadFile(networkWithPRADestinationPath, new ByteArrayInputStream(dataSource.getData(null, IIDM_EXTENSION)));
            networkWithPraFileUrl = minioAdapter.generatePreSignedUrl(networkWithPRADestinationPath);
            return networkWithPraFileUrl;
        } else {
            throw new RaoRunnerException("Cannot find optimized network in rao result");
        }
    }

    String exportAndSaveJsonRaoResult(RaoResult raoResult, Crac crac, String resultsDestination) {
        ByteArrayOutputStream outputStreamRaoResult = new ByteArrayOutputStream();
        new RaoResultExporter().export(raoResult, crac, outputStreamRaoResult);
        String raoResultDestinationPath = resultsDestination + File.separator + RAO_RESULT;
        minioAdapter.uploadFile(raoResultDestinationPath, new ByteArrayInputStream(outputStreamRaoResult.toByteArray()));
        return minioAdapter.generatePreSignedUrl(raoResultDestinationPath);
    }
}
