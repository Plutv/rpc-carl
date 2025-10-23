package org.example.config;

import lombok.*;
import org.example.client.serviceCenter.balance.ConsistencyHashBalance;
import org.example.common.serializer.mySerializer.Serializer;
import org.example.server.serviceRegister.impl.ZKServiceRegister;

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

    private String registry = new ZKServiceRegister().toString();

    private String serializer = Serializer.getSerializerByCode(1).toString();

    private String loadBalance = new ConsistencyHashBalance().toString();
}
