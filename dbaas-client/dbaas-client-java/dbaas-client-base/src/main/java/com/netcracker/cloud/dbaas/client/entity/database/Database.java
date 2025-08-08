package com.netcracker.cloud.dbaas.client.entity.database;

import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * The class used to invoke the API of {@link com.netcracker.cloud.dbaas.client.DbaasClient}
 * which can operate with any type of database known to dbaas running behind it
 * <pre>
 * for usage example see {@link com.netcracker.cloud.dbaas.client.entity.database.type.DatabaseType}
 * </pre>
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class Database extends AbstractDatabase<Map<String, Object>> {
}


