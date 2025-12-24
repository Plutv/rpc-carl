package org.example.trace.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.example.common.trace.TraceContext;
import org.example.trace.TraceIdGenerator;
import org.example.trace.ZipkinReporter;

@Slf4j
public class ClientTraceInterceptor {
    public static void beforeInvoke() {
        String traceId = TraceContext.getTraceId();
        if (traceId == null) {
            traceId = TraceIdGenerator.generateTraceId();
            TraceContext.setTraceId(traceId);
        }
        String spanId = TraceIdGenerator.generateSpanId();
        TraceContext.setSpanId(spanId);

        long timeStamp = System.currentTimeMillis();
        TraceContext.setStartTimeStamp(String.valueOf(timeStamp));
    }

    public static void afterInvoke(String serviceName) {
        long endTime = System.currentTimeMillis();
        long startTime = Long.parseLong(TraceContext.getStartTimeStamp());
        long duration = endTime - startTime;

        ZipkinReporter.reportSpan(
                TraceContext.getTraceId(),
                TraceContext.getSpanId(),
                TraceContext.getParentSpanId(),
                "client-" + serviceName,
                startTime,
                duration,
                serviceName,
                "client"
        );

        TraceContext.clear();
    }
}
