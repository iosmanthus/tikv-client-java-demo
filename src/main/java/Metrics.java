import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
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
    TiConfiguration conf = TiConfiguration.createRawDefault(
        "127.0.0.1:2379,127.0.0.1:2382,127.0.0.1:2384");
    conf.setApiVersion(ApiVersion.V2);
    conf.setEnableAtomicForCAS(true);
    conf.setEnableGrpcForward(true);
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
    conf.setMetricsEnable(true);
    conf.setMetricsPort(3141);

    ExecutorService executorService = Executors.newFixedThreadPool(64);

    try (TiSession session = TiSession.create(conf)) {
      CountDownLatch latch = new CountDownLatch(64);
      ArrayList<byte[]> keys = new ArrayList<>();
      for (int i = 0; i < 1000; i++) {
        keys.add(String.format("key@%d", i).getBytes());
      }
      session.splitRegionAndScatter(keys);
      try (RawKVClient client = session.createRawClient()) {
        for (int t = 0; t < 64; t++) {
          int finalT = t;
          executorService.submit(() -> {
            for (int i = 0; i < 100000000; i++) {
              try {
                client.put(ByteString.copyFromUtf8(String.format("key@%d", i)),
                    ByteString.copyFromUtf8(String.format("value@%d", i)));
                client.get(ByteString.copyFromUtf8("key@" + i));
                logger.info(finalT + " get " + i);
              } catch (Exception e) {
                logger.error("got error {}", e.toString());
              }
              try {
                Thread.sleep(100);
              } catch (Exception ignore) {
              }
            }
            latch.countDown();
          });
        }
        latch.await();
      }
    }
  }
}