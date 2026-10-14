package com.example.refund.mapper;

import com.example.governance.runtime.SensitiveMappingSupport;
import com.example.refund.dto.RefundApplicationResponse;
import com.example.refund.entity.RefundApplicationEntity;
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
