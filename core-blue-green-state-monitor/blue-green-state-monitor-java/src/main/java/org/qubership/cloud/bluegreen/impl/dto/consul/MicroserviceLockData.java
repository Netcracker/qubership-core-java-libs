package org.qubership.cloud.bluegreen.impl.dto.consul;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.qubership.cloud.bluegreen.impl.dto.serdes.OffsetDateTimeDeserializer;
import org.qubership.cloud.bluegreen.impl.dto.serdes.OffsetDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MicroserviceLockData {
    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
    private OffsetDateTime timestamp;
    private String reason;
}
