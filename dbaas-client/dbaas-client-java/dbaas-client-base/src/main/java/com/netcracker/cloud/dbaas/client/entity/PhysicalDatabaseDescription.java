package com.netcracker.cloud.dbaas.client.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PhysicalDatabaseDescription {
    private String adapterId;
    private Map<String, String> labels;
}
