package org.example.common.message;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class RpcResponse implements Serializable {
    private int code;
    private String message;
    private Object data;

    public static RpcResponse success(Object data) {
        return RpcResponse.builder().code(200).message("success").data(data).build();
    }

    public static RpcResponse fail() {
        return RpcResponse.builder().code(500).message("服务器错误").build();
    }
}
