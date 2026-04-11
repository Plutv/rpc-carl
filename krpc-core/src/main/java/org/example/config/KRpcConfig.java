package org.example.config;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class KRpcConfig {
    private String name = "krpc";

    private Integer port = 9999;

    private String host = "localhost";

    private String version = "1.0.0";

    private String registry = "zookeeper";

    private String serializer = "json";

    private String loadBalance = "consistencyHash";
}
