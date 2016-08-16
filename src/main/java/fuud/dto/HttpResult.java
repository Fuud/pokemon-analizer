package fuud.dto;

public class HttpResult {
    private final boolean result;
    private final String reason;

    public HttpResult(boolean result, String reason) {
        this.result = result;
        this.reason = reason;
    }

    public boolean isResult() {
        return result;
    }

    public String getReason() {
        return reason;
    }
}
