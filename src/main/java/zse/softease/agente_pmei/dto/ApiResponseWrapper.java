package zse.softease.agente_pmei.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResponseWrapper<T> {

    @JsonAlias({"success"})
    public Boolean success;

    @JsonAlias({"statusCode", "code"})
    public Integer statusCode;

    @JsonAlias({"status"})
    public Object status;

    @JsonAlias({"httpStatus", "http_status"})
    public String httpStatus;

    @JsonAlias({"message", "msg", "error"})
    public String message;

    public T data;

    public boolean isOk() {

        if (Boolean.TRUE.equals(success)) return true;

        if (statusCode != null) {
            return statusCode >= 200 && statusCode < 300;
        }

        if (status != null) {
            if (status instanceof Boolean) {
                return Boolean.TRUE.equals(status);
            }
            if (status instanceof Number) {
                int s = ((Number) status).intValue();
                return s >= 200 && s < 300;
            }
            if (status instanceof String) {
                if ("true".equalsIgnoreCase((String) status)) return true;
                try {
                    int s = Integer.parseInt((String) status);
                    return s >= 200 && s < 300;
                } catch (NumberFormatException ignored) {}
            }
        }

        return "OK".equalsIgnoreCase(httpStatus);
    }
}
