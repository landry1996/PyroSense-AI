package com.pyrosense.device.adapter.in.rest.dto;

public record DeviceStatisticsResponse(int total, int active, int offline, int provisioned, int revoked) {}
