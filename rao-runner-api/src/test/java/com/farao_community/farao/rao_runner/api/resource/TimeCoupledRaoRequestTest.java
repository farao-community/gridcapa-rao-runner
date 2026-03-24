package com.farao_community.farao.rao_runner.api.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class TimeCoupledRaoRequestTest {
    @Test
    void deserializeTest() throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());

        final TimeCoupledRaoRequest timeCoupledRaoRequest = objectMapper.readValue("""
            {
              "id": "8",
              "runId": "1234-56-7890",
              "instant": null,
              "raoParametersFileUrl": "/test_base_path/RaoParameters.json",
              "resultsDestination": "timecoupled_rao_results",
              "targetEndInstant": null,
              "eventPrefix": null,
              "icsFileUrl": "/test_base_path/timecoupled-constraints.json",
              "timedInputs": [
                {
                  "timestamp": "2019-01-08T00:30+01:00",
                  "networkFileUrl": "/test_base_path/initialNetwork_0030.xiidm",
                  "cracFileUrl": "/test_base_path/crac_0030.json"
                },
                {
                  "timestamp": "2019-01-08T01:30+01:00",
                  "networkFileUrl": "/test_base_path/initialNetwork_0130.xiidm",
                  "cracFileUrl": "/test_base_path/crac_0130.json"
                },
                {
                  "timestamp": "2019-01-08T02:30+01:00",
                  "networkFileUrl": "/test_base_path/initialNetwork_0230.xiidm",
                  "cracFileUrl": "/test_base_path/crac_0230.json"
                },
                {
                  "timestamp": "2019-01-08T03:30+01:00",
                  "networkFileUrl": "/test_base_path/initialNetwork_0330.xiidm",
                  "cracFileUrl": "/test_base_path/crac_0330.json"
                }
              ]
            }
            """, TimeCoupledRaoRequest.class);

        Assertions.assertThat(timeCoupledRaoRequest)
            .isNotNull()
            .hasFieldOrPropertyWithValue("id", "8")
            .hasFieldOrPropertyWithValue("runId", "1234-56-7890")
            .hasFieldOrPropertyWithValue("instant", Optional.empty())
            .hasFieldOrPropertyWithValue("raoParametersFileUrl", "/test_base_path/RaoParameters.json")
            .hasFieldOrPropertyWithValue("resultsDestination", Optional.of("timecoupled_rao_results"))
            .hasFieldOrPropertyWithValue("targetEndInstant", Optional.empty())
            .hasFieldOrPropertyWithValue("eventPrefix", Optional.empty())
            .hasFieldOrPropertyWithValue("icsFileUrl", "/test_base_path/timecoupled-constraints.json");
        Assertions.assertThat(timeCoupledRaoRequest.getTimedInputs()).hasSize(4);
        final SoftAssertions softly = new SoftAssertions();
        for (int i = 0; i < 4; i++) {
            final TimedInput timedInput = timeCoupledRaoRequest.getTimedInputs().get(i);
            softly.assertThat(timedInput.timestamp()).isEqualTo(OffsetDateTime.parse("2019-01-08T0" + i + ":30+01:00"));
            softly.assertThat(timedInput.networkFileUrl()).isEqualTo("/test_base_path/initialNetwork_0" + i + "30.xiidm");
            softly.assertThat(timedInput.cracFileUrl()).isEqualTo("/test_base_path/crac_0" + i + "30.json");
        }
        softly.assertAll();
    }

    @Test
    void serializeTest() throws IOException {
        final List<TimedInput> timedInputs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            timedInputs.add(new TimedInput(OffsetDateTime.parse("2019-01-08T0" + i + ":30:00+01:00"),
                                           "/test_base_url/initialNetwork_0" + i + "30.xiidm",
                                           "/test_base_url/crac_0" + i + "30.json"));
        }
        final TimeCoupledRaoRequest timeCoupledRaoRequest = new TimeCoupledRaoRequest.RaoRequestBuilder()
            .withId("8")
            .withRunId("1234-56-7890")
            .withTimedInputs(timedInputs)
            .withIcsFileUrl("/timecoupled_rao_inputs/simple_case/timecoupled-constraints.json")
            .withRaoParametersFileUrl("/timecoupled_rao_inputs/simple_case/RaoParameters.json")
            .withResultsDestination("timecoupled_rao_results")
            .build();

        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());

        final String serializedTimeCoupledRaoRequest = objectMapper.writeValueAsString(timeCoupledRaoRequest);
        Assertions.assertThat(serializedTimeCoupledRaoRequest)
            .isEqualTo("{\"id\":\"8\",\"runId\":\"1234-56-7890\",\"instant\":null,\"raoParametersFileUrl\":\"/timecoupled_rao_inputs/simple_case/RaoParameters.json\",\"resultsDestination\":\"timecoupled_rao_results\",\"targetEndInstant\":null,\"eventPrefix\":null,\"icsFileUrl\":\"/timecoupled_rao_inputs/simple_case/timecoupled-constraints.json\",\"timedInputs\":[{\"timestamp\":\"2019-01-08T00:30+01:00\",\"networkFileUrl\":\"/test_base_url/initialNetwork_0030.xiidm\",\"cracFileUrl\":\"/test_base_url/crac_0030.json\"},{\"timestamp\":\"2019-01-08T01:30+01:00\",\"networkFileUrl\":\"/test_base_url/initialNetwork_0130.xiidm\",\"cracFileUrl\":\"/test_base_url/crac_0130.json\"},{\"timestamp\":\"2019-01-08T02:30+01:00\",\"networkFileUrl\":\"/test_base_url/initialNetwork_0230.xiidm\",\"cracFileUrl\":\"/test_base_url/crac_0230.json\"},{\"timestamp\":\"2019-01-08T03:30+01:00\",\"networkFileUrl\":\"/test_base_url/initialNetwork_0330.xiidm\",\"cracFileUrl\":\"/test_base_url/crac_0330.json\"}]}");
    }
}
