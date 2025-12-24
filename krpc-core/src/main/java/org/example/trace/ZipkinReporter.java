package org.example.trace;

import lombok.extern.slf4j.Slf4j;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

@Slf4j
public class ZipkinReporter {
    private static final String ZIPKIN_URL = "http://localhost:9411/api/v2/spans";

    private static final AsyncReporter<Span> reporter;

    static {
        OkHttpSender sender = OkHttpSender.create(ZIPKIN_URL);
        reporter = AsyncReporter.create(sender);
    }

    public static void reportSpan(String traceId, String spanId, String parentSpanId,
                                  String name, long startTimeStamp, long duration,
                                  String serviceName, String type) {
        Span span = Span.newBuilder().traceId(traceId)
                .id(spanId).parentId(parentSpanId).name(name).timestamp(startTimeStamp * 1000)
                .duration(duration * 1000).putTag("serviceName", serviceName).putTag("type", type).build();

        reporter.report(span);
        log.info("当前traceId = {} 正在上报日志", traceId);
    }

    public static void close() {
        reporter.close();
    }

}
