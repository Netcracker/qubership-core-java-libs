package org.qubership.cloud.core.error.rest.tmf;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.qubership.cloud.core.error.runtime.ErrorCodeException;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TmfErrorResponse {
    public static final String TYPE_V1_0 = "NC.TMFErrorResponse.v1.0";

    private String id;
    private String referenceError;
    private String code;
    private String reason;
    @JsonProperty("message")
    private String detail;
    private String status;
    private Object source;
    private Map<String, Object> meta = new TreeMap<>();
    private List<TmfError> errors = new ArrayList<>();
    @JsonProperty("@type")
    private String type;
    @JsonProperty("@schemaLocation")
    private String schemaLocation;

    public static TmfErrorResponseBuilder builder() {
        return new TmfErrorResponseBuilder();
    }
    public static TmfErrorResponseBuilder builder(ErrorCodeException e) {
        if (e == null) {
            throw new IllegalArgumentException("ErrorCodeException cannot be null");
        }
        TmfErrorResponseBuilder builder = new TmfErrorResponseBuilder();
        builder.id = e.getId();
        builder.code = e.getErrorCode().getCode();
        builder.reason = e.getErrorCode().getTitle();
        builder.detail = e.getDetail();
        builder.type = TYPE_V1_0;
        return builder;
    }

    public static class TmfErrorResponseBuilder {
        private String id;
        private String referenceError;
        private String code;
        private String reason;
        private String detail;
        private String status;
        private Object source;
        private Map<String, Object> meta;
        private List<TmfError> errors;
        private String type;
        private String schemaLocation;

        TmfErrorResponseBuilder() {
        }

        public TmfErrorResponseBuilder id(String id) {
            this.id = id;
            return this;
        }

        public TmfErrorResponseBuilder referenceError(String referenceError) {
            this.referenceError = referenceError;
            return this;
        }

        public TmfErrorResponseBuilder code(String code) {
            this.code = code;
            return this;
        }

        public TmfErrorResponseBuilder reason(String reason) {
            this.reason = reason;
            return this;
        }

        @JsonProperty("message")
        public TmfErrorResponseBuilder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public TmfErrorResponseBuilder status(String status) {
            this.status = status;
            return this;
        }

        public TmfErrorResponseBuilder source(Object source) {
            this.source = source;
            return this;
        }

        public TmfErrorResponseBuilder meta(Map<String, Object> meta) {
            this.meta = meta;
            return this;
        }

        public TmfErrorResponseBuilder errors(List<TmfError> errors) {
            this.errors = errors;
            return this;
        }

        @JsonProperty("@type")
        public TmfErrorResponseBuilder type(String type) {
            this.type = type;
            return this;
        }

        @JsonProperty("@schemaLocation")
        public TmfErrorResponseBuilder schemaLocation(String schemaLocation) {
            this.schemaLocation = schemaLocation;
            return this;
        }

        public TmfErrorResponse build() {
            return new TmfErrorResponse(this.id, this.referenceError, this.code, this.reason, this.detail, this.status, this.source, this.meta, this.errors, this.type, this.schemaLocation);
        }

        public String toString() {
            return "TmfErrorResponse.TmfErrorResponseBuilder(id=" + this.id + ", referenceError=" + this.referenceError + ", code=" + this.code + ", reason=" + this.reason + ", detail=" + this.detail + ", status=" + this.status + ", source=" + this.source + ", meta=" + this.meta + ", errors=" + this.errors + ", type=" + this.type + ", schemaLocation=" + this.schemaLocation + ")";
        }
    }
}
