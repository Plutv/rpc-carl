package org.example.trace.interceptor;

import org.example.common.trace.TraceContext;
import org.example.trace.TraceIdGenerator;
import org.example.trace.ZipkinReporter;

public class ServerTraceInterceptor {
    public static void beforeHandle() {
        String traceId = TraceContext.getTraceId();
        String parentSpanId = TraceContext.getParentSpanId();
        String spanId = TraceIdGenerator.generateSpanId();

        // TODO：获取又设置？
        TraceContext.setTraceId(traceId);
        TraceContext.setParentSpanId(parentSpanId);
        TraceContext.setSpanId(spanId);

        long startTimeStamp = System.currentTimeMillis();
        TraceContext.setStartTimeStamp(String.valueOf(startTimeStamp));
    }

    public static void afterHandle(String serviceName) {
        long endTimeStamp = System.currentTimeMillis();
        long startTimeStamp = Long.valueOf(TraceContext.getStartTimeStamp());
        long duration = endTimeStamp - startTimeStamp;

        ZipkinReporter.reportSpan(
                TraceContext.getTraceId(),
                TraceContext.getSpanId(),
                TraceContext.getParentSpanId(),
                "server-" + serviceName,
                startTimeStamp,
                duration,
                serviceName,
                "server"
        );

        TraceContext.clear();
    }
}
