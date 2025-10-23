package org.example.common.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.dialect.Props;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfigUtil {
    public static<T> T loadConfig(Class<T> targetClass, String prefix) {
        return loadConfig(targetClass, prefix, "");
    }

    public static<T> T loadConfig(Class<T> targetClass, String prefix, String environment) {
        StringBuilder configFileNameBuilder = new StringBuilder("application");

        if (StrUtil.isNotBlank(environment)) {
            configFileNameBuilder.append("-").append(environment);
        }
        configFileNameBuilder.append(".properties");

        Props properties = new Props(configFileNameBuilder.toString());

        if (properties.isEmpty()) {
            log.warn("配置文件'{}'为空，或加载失败！", configFileNameBuilder.toString());
        } else {
            log.info("加载配置文件，'{}'", configFileNameBuilder.toString());
        }

        try {
            return properties.toBean(targetClass, prefix);
        } catch (Exception e) {
            log.error("配置转换失败，目标类：{}", targetClass.getName(), e);
            throw new RuntimeException("配置加载失败", e);
        }
    }
}
