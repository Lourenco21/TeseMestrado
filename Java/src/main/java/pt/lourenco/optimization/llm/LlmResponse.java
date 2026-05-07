package pt.lourenco.optimization.llm;

public class LlmResponse {

    private final String model;
    private final String content;
    private final int httpStatusCode;
    private final long durationMs;
    private final String rawResponse;
    private final String error;

    public LlmResponse(String model, String content, int httpStatusCode, long durationMs, String rawResponse, String error) {
        this.model = model;
        this.content = content;
        this.httpStatusCode = httpStatusCode;
        this.durationMs = durationMs;
        this.rawResponse = rawResponse;
        this.error = error;
    }

    public String getModel() {
        return model;
    }

    public String getContent() {
        return content;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public String getError() {
        return error;
    }

    public boolean hasError() {
        return error != null && !error.isBlank();
    }
}