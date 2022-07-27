import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tikv.common.TiConfiguration;
import org.tikv.common.TiConfiguration.ApiVersion;
import org.tikv.common.TiSession;
import org.tikv.raw.RawKVClient;
import org.tikv.shade.com.google.protobuf.ByteString;

public class Metrics {
  private static final Logger logger = LoggerFactory.getLogger(Metrics.class);

  public static void main(String[] args) throws Exception {
    TiConfiguration conf = TiConfiguration.createRawDefault();
    conf.setApiVersion(ApiVersion.V2);
    conf.setEnableAtomicForCAS(true);
    conf.setTimeout(150);
    conf.setForwardTimeout(200);
    conf.setRawKVReadTimeoutInMS(400);
    conf.setRawKVWriteTimeoutInMS(400);
    conf.setRawKVBatchReadTimeoutInMS(400);
    conf.setRawKVBatchWriteTimeoutInMS(400);
    conf.setRawKVWriteSlowLogInMS(50);
    conf.setRawKVReadSlowLogInMS(50);
    conf.setRawKVBatchReadSlowLogInMS(50);
    conf.setRawKVBatchWriteSlowLogInMS(50);
    conf.setCircuitBreakEnable(false);

    ExecutorService executorService = Executors.newFixedThreadPool(16);

    try (TiSession session = TiSession.create(conf)) {
      try (RawKVClient client = session.createRawClient()) {
        for (int t = 0; t < 8; t++) {
          int finalT = t;
          executorService.submit(() -> {
            for (int i = 0; i < 100000000; i++) {
              try {
                client.put(ByteString.copyFromUtf8("key@" + i), ByteString.copyFromUtf8("value"));
                client.get(ByteString.copyFromUtf8("key@" + i));
                logger.info(finalT + " put " + i);
              } catch (Exception e) {
                logger.error(e.toString());
              }
              try {
                Thread.sleep(100);
              } catch (Exception ignore) {
              }
            }
          });
        }
      }
    }
  }
}