package com.pyrosense.device.adapter.in.rest.dto;

import java.util.List;

public record DevicePageResponse(
        List<DeviceResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {}
