package com.netcracker.cloud.dbaas.client.opensearch.entity;

import com.netcracker.cloud.dbaas.client.entity.database.AbstractDatabase;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class OpensearchIndex extends AbstractDatabase<OpensearchIndexConnection> {
}
