package com.example.camundatutorial.refund;

import com.example.camundatutorial.governance.SensitiveMappingSupport;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RefundApplicationMapper {

    private final SensitiveMappingSupport mappingSupport;

    public RefundApplicationMapper(SensitiveMappingSupport mappingSupport) {
        this.mappingSupport = mappingSupport;
    }

    public RefundApplicationResponse toResponse(RefundApplicationEntity entity) {
        if (entity == null) {
            return null;
        }
        RefundApplicationResponse response = new RefundApplicationResponse();
        mappingSupport.copyApplyingPolicy(entity, response);
        return response;
    }

    public List<RefundApplicationResponse> toResponseList(List<RefundApplicationEntity> entities) {
        return entities.stream().map(this::toResponse).toList();
    }
}
