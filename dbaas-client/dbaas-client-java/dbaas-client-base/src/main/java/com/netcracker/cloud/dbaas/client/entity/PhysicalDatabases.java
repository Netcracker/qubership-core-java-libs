package com.netcracker.cloud.dbaas.client.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PhysicalDatabases {
    private Map<String, PhysicalDatabaseDescription> identified;
}
