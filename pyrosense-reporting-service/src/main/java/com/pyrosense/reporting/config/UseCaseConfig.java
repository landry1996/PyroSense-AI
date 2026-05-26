package com.pyrosense.reporting.config;

import com.pyrosense.reporting.application.port.out.*;
import com.pyrosense.reporting.application.usecase.GenerateReportService;
import com.pyrosense.reporting.application.usecase.GetReportService;
import com.pyrosense.reporting.application.usecase.RequestReportService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public GenerateReportService generateReportService(ReportRepositoryPort repository,
                                                       ReportRendererPort renderer,
                                                       PdfRendererPort professionalRenderer,
                                                       ReportDataProviderPort dataProvider,
                                                       ReportEventPublisherPort eventPublisher) {
        return new GenerateReportService(repository, renderer, professionalRenderer, dataProvider, eventPublisher);
    }

    @Bean
    public GetReportService getReportService(ReportRepositoryPort repository,
                                             DownloadTokenStorePort tokenStore) {
        return new GetReportService(repository, tokenStore);
    }

    @Bean
    public RequestReportService requestReportService(GenerateReportService generateService,
                                                     ReportRepositoryPort repository,
                                                     FileStoragePort fileStorage,
                                                     ReportAuditLogPort auditLog) {
        return new RequestReportService(generateService, repository, fileStorage, auditLog);
    }
}
