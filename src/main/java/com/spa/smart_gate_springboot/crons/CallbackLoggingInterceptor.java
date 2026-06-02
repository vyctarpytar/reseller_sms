package com.spa.smart_gate_springboot.crons;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

import java.io.IOException;

/**
 * Logs delivery-report callbacks as JSON: the outgoing request body and the response
 * body (pretty-printed when they are valid JSON, otherwise logged raw — e.g. an nginx
 * 403 HTML page). Lets you inspect exactly what was sent and what came back.
 */
@Slf4j
public class CallbackLoggingInterceptor implements Interceptor {

    private final ObjectMapper objectMapper;

    public CallbackLoggingInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String requestBody = readRequestBody(request);

        log.info("Callback REQUEST -> {} {}\n{}", request.method(), request.url(), toJson(requestBody));

        Response response;
        try {
            response = chain.proceed(request);
        } catch (IOException e) {
            log.warn("Callback REQUEST -> {} {} failed before a response: {}",
                    request.method(), request.url(), e.getMessage());
            throw e;
        }

        // Read the response body, then rebuild it so Retrofit can still consume it.
        ResponseBody body = response.body();
        MediaType contentType = body != null ? body.contentType() : null;
        String responseBody = body != null ? body.string() : "";

        log.info("Callback RESPONSE <- {} {} status={}\n{}",
                request.method(), request.url(), response.code(), toJson(responseBody));

        ResponseBody rebuilt = ResponseBody.create(responseBody, contentType);
        return response.newBuilder().body(rebuilt).build();
    }

    private String readRequestBody(Request request) {
        try {
            if (request.body() == null) {
                return "";
            }
            Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            return buffer.readUtf8();
        } catch (Exception e) {
            return "";
        }
    }

    /** Pretty-print as JSON; if the content is not JSON, return it unchanged. */
    private String toJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        try {
            Object tree = objectMapper.readValue(raw, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
        } catch (Exception e) {
            return raw;
        }
    }
}
