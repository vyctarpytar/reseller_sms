package com.spa.smart_gate_springboot.config.intercept;



import com.spa.smart_gate_springboot.config.logging.*;
import com.spa.smart_gate_springboot.config.logging.trace.RequestTrace;
import com.spa.smart_gate_springboot.config.logging.trace.RequestTraceContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.Buffer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;



@Slf4j
@Getter
public class HttpPublisherInterceptor implements Interceptor {

    private static final String CF_RAY_HTTP_HEADER = "cf-ray";

    private static final String HTTP_REQUEST_RESPONSE = "HTTP-OUT REQUEST: [{}] with body [{}], RESPONSE: status [{}] body [{}]";
    private static final String ERROR_HTTP_REQUEST_RESPONSE = "HTTP-OUT REQUEST: [{}] with body [{}], RESPONSE: exception [{}]";
    private static final String ERROR_REQUEST_URL_NOT_DEFINED = "Request URL Not Defined";
    private static final String ERROR_REQUEST_BODY_NOT_DEFINED = "Request Body Not Defined";

    public HttpPublisherInterceptor() {
    }

    @Override
    public Response intercept(final Chain chain) {

        final Request request = buildRequest(chain);
        final String requestUrl = getRequestUrl(request);
        final String requestBody = getRequestBody(request);

        try {

            final Response response = chain.proceed(request);
            final MediaType contentType = response.body().contentType();
            final String content = response.body().string();

            getLoggerConsumer(getHttpStatus(response.code())).accept(log, LoggingContext.builder()
                    .format(HTTP_REQUEST_RESPONSE)
                    .arguments(new Object[]{
                            requestUrl, requestBody, response.code(), content
                    })
                    .flowLoggingContext(HttpLoggingContext.outHttpContext(getHttpStatus(response.code())))
                    .build());

            final ResponseBody wrappedBody = ResponseBody.create(contentType, content);

            return response.newBuilder().body(wrappedBody).build();

        } catch (final Throwable throwable) {

            LoggingFactory.error(log, HttpLoggingContext.outHttpContext(Optional.empty()),
                    ERROR_HTTP_REQUEST_RESPONSE,
                    requestUrl,
                    requestBody,
                    ExceptionUtils.getStackTrace(throwable));

            throw new IllegalStateException(throwable);
        }
    }

    private Request buildRequest(final Chain chain) {

        final Optional<RequestTrace> requestTrace = RequestTraceContext.getCurrentTrace();

        if (requestTrace.isPresent() && StringUtils.isNotBlank(requestTrace.get().getRequestId())) {

            return chain.request()
                    .newBuilder()
                    .addHeader(CF_RAY_HTTP_HEADER, requestTrace.get().getRequestId())
                    .build();
        }

        return chain.request();
    }

    private Optional<HttpStatus> getHttpStatus(final int code) {

        try {

            return Optional.of(HttpStatus.valueOf(code));
        } catch (final Throwable throwable) {

            return Optional.empty();
        }
    }

    private BiConsumer<Logger, LoggingContext> getLoggerConsumer(final Optional<HttpStatus> httpStatus) {

        if (!httpStatus.isPresent()) {

            return LoggingFactory::error;
        }

        if (httpStatus.get().is5xxServerError()) {

            return LoggingFactory::error;
        }

        if (httpStatus.get().is4xxClientError()) {

            return LoggingFactory::warn;
        }

        return LoggingFactory::info;
    }

    private String getRequestUrl(final Request request) {

        try {

            return request.url().toString();
        } catch (final Throwable throwable) {

            return ERROR_REQUEST_URL_NOT_DEFINED;
        }
    }

    private String getRequestBody(final Request request) {

        try {

            final Buffer requestBuffer = new Buffer();
            if (Objects.nonNull(request.body())) {

                request.body().writeTo(requestBuffer);
            }

            return requestBuffer.readUtf8();
        } catch (final Throwable throwable) {

            return ERROR_REQUEST_BODY_NOT_DEFINED;
        }
    }
}