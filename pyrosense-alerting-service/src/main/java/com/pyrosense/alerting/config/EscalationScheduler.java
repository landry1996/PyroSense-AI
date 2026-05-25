package com.pyrosense.alerting.config;

import com.pyrosense.alerting.application.port.in.EscalateAlertsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class EscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(EscalationScheduler.class);

    private final EscalateAlertsUseCase escalateAlertsUseCase;

    public EscalationScheduler(EscalateAlertsUseCase escalateAlertsUseCase) {
        this.escalateAlertsUseCase = escalateAlertsUseCase;
    }

    @Scheduled(fixedDelayString = "${pyrosense.alerting.escalation-check-interval:300000}")
    public void checkEscalations() {
        int count = escalateAlertsUseCase.escalateOverdueAlerts();
        if (count > 0) {
            log.info("Escalated {} overdue alerts", count);
        }
    }
}
