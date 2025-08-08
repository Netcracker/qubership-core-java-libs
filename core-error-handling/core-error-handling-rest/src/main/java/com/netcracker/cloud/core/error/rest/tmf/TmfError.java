package org.qubership.cloud.core.error.rest.tmf;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.qubership.cloud.core.error.runtime.ErrorCodeException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.TreeMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TmfError {
    private String id;
    private String referenceError;
    private String code;
    private String reason;
    @JsonProperty("message")
    private String detail;
    private String status;
    private Object source;
    private Map<String, Object> meta = new TreeMap<>();

    public static TmfErrorBuilder builder() {
        return new TmfErrorBuilder();
    }
    public static TmfErrorBuilder builder(ErrorCodeException e) {
        if (e == null) {
            throw new IllegalArgumentException("ErrorCodeException cannot be null");
        }
        TmfErrorBuilder builder = new TmfErrorBuilder();
        builder.id = e.getId();
        builder.code = e.getErrorCode().getCode();
        builder.reason = e.getErrorCode().getTitle();
        builder.detail = e.getDetail();
        return builder;
    }

    public static class TmfErrorBuilder {
        private String id;
        private String referenceError;
        private String code;
        private String reason;
        private String detail;
        private String status;
        private Object source;
        private Map<String, Object> meta;

        TmfErrorBuilder() {
        }

        public TmfErrorBuilder id(String id) {
            this.id = id;
            return this;
        }

        public TmfErrorBuilder referenceError(String referenceError) {
            this.referenceError = referenceError;
            return this;
        }

        public TmfErrorBuilder code(String code) {
            this.code = code;
            return this;
        }

        public TmfErrorBuilder reason(String reason) {
            this.reason = reason;
            return this;
        }

        @JsonProperty("message")
        public TmfErrorBuilder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public TmfErrorBuilder status(String status) {
            this.status = status;
            return this;
        }

        public TmfErrorBuilder source(Object source) {
            this.source = source;
            return this;
        }

        public TmfErrorBuilder meta(Map<String, Object> meta) {
            this.meta = meta;
            return this;
        }

        public TmfError build() {
            return new TmfError(this.id, this.referenceError, this.code, this.reason, this.detail, this.status, this.source, this.meta);
        }

        public String toString() {
            return "TmfError.TmfErrorBuilder(id=" + this.id + ", referenceError=" + this.referenceError + ", code=" + this.code + ", reason=" + this.reason + ", detail=" + this.detail + ", status=" + this.status + ", source=" + this.source + ", meta=" + this.meta + ")";
        }
    }
}
