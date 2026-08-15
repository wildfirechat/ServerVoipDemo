package cn.wildfirechat.voipdemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;

/**
 * 服務啟動完成後打印所有已註冊的 HTTP 接口，用於核對請求路徑是否正確。
 */
@Component
public class EndpointLogger implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(EndpointLogger.class);

    private final RequestMappingHandlerMapping handlerMapping;

    public EndpointLogger(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @Override
    public void run(ApplicationArguments args) {
        LOG.info("===== registered endpoints =====");
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
            HandlerMethod method = entry.getValue();
            LOG.info("{} -> {}#{}", entry.getKey(), method.getBeanType().getSimpleName(), method.getMethod().getName());
        }
        LOG.info("================================");
    }
}
