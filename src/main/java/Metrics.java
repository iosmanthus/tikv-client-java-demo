import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tikv.common.TiConfiguration;
import org.tikv.common.TiSession;
import org.tikv.common.util.BackOffer;
import org.tikv.common.util.ConcreteBackOffer;
import org.tikv.raw.RawKVClient;
import org.tikv.shade.com.google.protobuf.ByteString;

public class Metrics {
  private static final Logger logger = LoggerFactory.getLogger(Metrics.class);

  public static void main(String[] args) throws Exception {
    TiConfiguration conf = TiConfiguration.createRawDefault(
        "172.16.5.32:2879,172.16.5.32:10082,172.16.5.32:10080");
    int timeout = 10000;
    conf.setTimeout(timeout);
    conf.setGrpcHealthCheckTimeout(timeout);
    conf.setRawKVWriteTimeoutInMS(timeout);
    conf.setEnableGrpcForward(true);
    conf.setRawKVReadTimeoutInMS(timeout);
    conf.setEnableAtomicForCAS(true);
    conf.setTlsEnable(true);
    conf.setTrustCertCollectionFile("src/test/resources/ca.crt");
    conf.setKeyCertChainFile("src/test/resources/client.crt");
    conf.setKeyFile("src/test/resources/client-java.pem");

    TiSession session = TiSession.create(conf);
    RawKVClient client = session.createRawClient();
    for (int i = 0; i < 10; i++) {
      logger.info("get");
      try {
        BackOffer backOffer = ConcreteBackOffer.newCustomBackOff(100);
        client.get(ByteString.EMPTY);
        session.getRegionManager().invalidateAll();
        session.getPDClient().getRegionByKey(backOffer, ByteString.EMPTY);
        session.getRegionManager().invalidateAll();
      } catch (Exception e) {
        logger.error(e.toString());
      }
      Thread.sleep(100);
    }
    session.close();
  }
}