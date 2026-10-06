package com.example.camundatutorial.refund;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("setDrnAndStationDelegate")
public class SetDrnAndStationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(SetDrnAndStationDelegate.class);

    private static final List<String> STATIONS = List.of(
            "STATION-NORTH",
            "STATION-SOUTH",
            "STATION-EAST",
            "STATION-WEST",
            "STATION-HQ"
    );

    private final RefundApplicationService refundApplicationService;

    public SetDrnAndStationDelegate(RefundApplicationService refundApplicationService) {
        this.refundApplicationService = refundApplicationService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String applicationId = String.valueOf(execution.getVariable("applicationId"));
        int sequence = ThreadLocalRandom.current().nextInt(100000, 999999);
        String drn = "DRN-" + sequence;
        String station = STATIONS.get(ThreadLocalRandom.current().nextInt(STATIONS.size()));

        refundApplicationService.update(applicationId, entity -> {
            entity.setDrn(drn);
            entity.setStation(station);
            entity.setStatus("ASSIGNED");
        });

        execution.setVariable("drn", drn);
        execution.setVariable("station", station);
        execution.setVariable("applicationStatus", "ASSIGNED");

        log.info("Assigned DRN {} and station {} to application {}", drn, station, applicationId);
    }
}
