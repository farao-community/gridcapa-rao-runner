package com.farao_community.farao.rao_runner.app;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.powsybl.openrao.commons.opentelemetry.OpenTelemetryReporter;

@Configuration
@ConditionalOnProperty(name = "management.tracing.enabled", havingValue = "true")
public class OpenTelemetryConfiguration {

  @Value("${spring.application.name}")
  private String serviceName;

  @Value("${otel.exporter.otlp.endpoint}")
  private String otlpEndpoint;

  @Value("${otel.exporter.export-logs:false}")
  private boolean otlpExportLogs;

  @Value("${spring.application.git.version}")
  private String gitVersion;

  @Bean
  public OpenTelemetry openTelemetry(SdkTracerProvider tracerProvider) {
    OpenTelemetryReporter.setOpenTelemetryTracer(tracerProvider, otlpExportLogs);
    return OpenTelemetrySdk.builder()
        .setTracerProvider(tracerProvider)
        .build();
  }

  @Bean
  public OtlpGrpcSpanExporter otlpGrpcSpanExporter() {
    return OtlpGrpcSpanExporter.builder().setEndpoint(otlpEndpoint).build();
  }

  @Bean
  public SdkTracerProvider sdkTracerProvider(OtlpGrpcSpanExporter spanExporter) {
    return SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
        .addSpanProcessor(new GitVersionSpanProcessor(gitVersion))
        .setResource(Resource.getDefault().merge(
            Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, serviceName))
        ))
        .build();
  }

}