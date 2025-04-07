package com.spa.smart_gate_springboot.config.intercept;



import com.spa.smart_gate_springboot.config.logging.HttpLoggingContext;
import com.spa.smart_gate_springboot.config.logging.LoggingContext;
import com.spa.smart_gate_springboot.config.logging.LoggingFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;


@Slf4j
@Component
public class LoggableDispatcherServlet extends DispatcherServlet {

    private static final String ERROR_LOGGING_HTTP_REQUEST = "Error logging http request";
    private static final String HTTP_REQUEST_HEADER_FILTER = "x_";
    private static final String RESPONSE_BODY_FILE_RESPONSE = "The Response Body contains a file response";
    private static final String HTTP_REQUEST_RESPONSE = "HTTP-IN - REQUEST: {} {} "
        + "Headers: [{}] "
        + "with Payload [{}], RESPONSE: {} with payload [{}] in "
        + "[{}] ms";

    @Override
    protected void doDispatch(final HttpServletRequest request, final HttpServletResponse response) {

        final ContentCachingRequestWrapper requestWrapper = getContentCachingRequestWrapper(request);

        final ContentCachingResponseWrapper responseWrapper = getContentCachingResponseWrapper(response);

        final StopWatch stopWatch = new StopWatch();

        try {
            stopWatch.start();
            super.doDispatch(requestWrapper, responseWrapper);
        } catch (final Exception ex) {
            logger.error(ERROR_LOGGING_HTTP_REQUEST, ex);
        } finally {
            try {
                stopWatch.stop();

                logHttpRequestResponse(requestWrapper, responseWrapper, stopWatch.getTotalTimeMillis());

                responseWrapper.copyBodyToResponse();
            } catch (final Exception ex) {
                logger.error(ERROR_LOGGING_HTTP_REQUEST, ex);
            }
        }
    }

    private ContentCachingRequestWrapper getContentCachingRequestWrapper(final HttpServletRequest request) {

        if (request instanceof ContentCachingRequestWrapper) {

            return (ContentCachingRequestWrapper) request;
        }

        return new ContentCachingRequestWrapper(request);
    }

    private ContentCachingResponseWrapper getContentCachingResponseWrapper(final HttpServletResponse response) {

        if (response instanceof ContentCachingResponseWrapper) {

            return (ContentCachingResponseWrapper) response;
        }

        return new ContentCachingResponseWrapper(response);
    }

    private void logHttpRequestResponse(final ContentCachingRequestWrapper request, final ContentCachingResponseWrapper response,
        final long totalTimeMs) {

        final HttpStatus responseHttpStatus = HttpStatus.valueOf(response.getStatus());


        try {

            final LoggingContext loggingContext = LoggingContext.builder()
                .format(HTTP_REQUEST_RESPONSE)
                .arguments(new Object[]{
                    request.getMethod(),
                    buildPathFromRequest(request),

                    buildHeaders(request),
                    buildRequestBody(request),
                    response.getStatus(),
                    buildResponseBody(response),
                    totalTimeMs
                })
                .flowLoggingContext(HttpLoggingContext.inHttpContext(responseHttpStatus))
                .build();

            getLoggerConsumer(responseHttpStatus, request.getMethod()).accept(log, loggingContext);

        } catch (final UnsupportedEncodingException ex) {

            LoggingFactory.error(log, HttpLoggingContext.inHttpContext(responseHttpStatus),
                ExceptionUtils.getStackTrace(ex));
        }
    }

    private String buildPathFromRequest(final ContentCachingRequestWrapper request) {

        if (StringUtils.isBlank(request.getQueryString())) {

            return request.getRequestURI();
        }

        return String.format("%s?%s", request.getRequestURI(), request.getQueryString());
    }



    private String buildHeaders(final ContentCachingRequestWrapper request) {

        return Collections.list(request.getHeaderNames())
            .stream()
            .filter(headerName -> headerName.toLowerCase().startsWith(HTTP_REQUEST_HEADER_FILTER))
            .map(headerName -> headerName + ": " + request.getHeader(headerName))
            .collect(Collectors.joining(","));
    }

    private String buildRequestBody(final ContentCachingRequestWrapper request) throws UnsupportedEncodingException {

        return new String(request.getContentAsByteArray(), 0, request.getContentAsByteArray().length, request.getCharacterEncoding());
    }

    private String buildResponseBody(final ContentCachingResponseWrapper response) throws UnsupportedEncodingException {

        if (contentTypeIsAFile(response)) {

            return RESPONSE_BODY_FILE_RESPONSE;
        }

        return new String(response.getContentAsByteArray(), 0, response.getContentAsByteArray().length, response.getCharacterEncoding());
    }

    private boolean contentTypeIsAFile(final ContentCachingResponseWrapper response) {

        final List<String> contentTypes = List.of("CSV_CONTENT_TYPE", "XLSX_CONTENT_TYPE");

        return Objects.nonNull(response.getContentType()) && contentTypes.contains(response.getContentType());
    }

    private BiConsumer<Logger, LoggingContext> getLoggerConsumer(final HttpStatus httpStatus, final String httpMethod) {

        if (httpStatus.is2xxSuccessful()) {

            return shouldLogAsDebug(httpMethod) ? LoggingFactory::debug : LoggingFactory::info;
        }

        if (httpStatus.is5xxServerError()) {

            return LoggingFactory::error;
        }

        return LoggingFactory::warn;
    }

    private boolean shouldLogAsDebug(final String httpMethod) {

        return HttpMethod.OPTIONS.matches(httpMethod);
    }

}
