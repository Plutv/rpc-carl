import org.example.common.util.ConfigUtil;
import org.example.config.KRpcConfig;

public class ConsumerTestConfig {
    public static void main(String[] args) {
        KRpcConfig rpc = ConfigUtil.loadConfig(KRpcConfig.class, "rpc");
        System.out.println(rpc);
    }
}
