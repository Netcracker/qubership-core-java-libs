package com.netcracker.cloud.dbaas.client.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.util.Map;

@Data
@AllArgsConstructor
public class GetDatabaseByClassifierRequest {
    @NonNull
    private Map<String, Object> classifier;
    private String userRole;
}
