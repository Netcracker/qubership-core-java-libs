package com.netcracker.cloud.maas.client.impl.dto.conf;

import lombok.Data;

@Data
public abstract class ConfigureResponse<R extends ConfigResource, T> {
    R request;
    ConfigureResult<T> result;
}
