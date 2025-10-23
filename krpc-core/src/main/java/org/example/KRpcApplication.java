package org.example;

import lombok.extern.slf4j.Slf4j;
import org.example.common.util.ConfigUtil;
import org.example.config.KRpcConfig;
import org.example.config.RpcConstant;

@Slf4j
public class KRpcApplication {
    private static volatile KRpcConfig rpcConfigInstance;

    public static void initialize(KRpcConfig customRpcConfig) {
        rpcConfigInstance = customRpcConfig;
        log.info("RPC 框架初始化, 配置{}", customRpcConfig);
    }

    public static void initialize() {
        KRpcConfig customRpcConfig;
        try {
            customRpcConfig = ConfigUtil.loadConfig(KRpcConfig.class, RpcConstant.CONFIG_FILE_PREFIX);
            log.info("成功加载配置文件，配置文件名称：{}", RpcConstant.CONFIG_FILE_PREFIX);
        } catch (Exception e) {
            customRpcConfig = new KRpcConfig();
            log.warn("配置加载失败，使用默认配置");
        }
        initialize(customRpcConfig);
    }

    public static KRpcConfig getRpcConfig() {
        if (rpcConfigInstance == null) {
            synchronized (KRpcApplication.class) {
                if (rpcConfigInstance == null) {
                    initialize(); // 第一次调用时初始化
                }
            }
        }

        return rpcConfigInstance;
    }
}
