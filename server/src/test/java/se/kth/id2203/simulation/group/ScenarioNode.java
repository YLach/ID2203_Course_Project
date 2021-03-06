package se.kth.id2203.simulation.group;

import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.bootstrapping.component.BootstrapClient;
import se.kth.id2203.bootstrapping.component.BootstrapServer;
import se.kth.id2203.bootstrapping.port.Bootstrapping;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

/**
 * Created by ralambom on 16/02/17.
 */
public class ScenarioNode extends ComponentDefinition {

    final static Logger LOG = LoggerFactory.getLogger(ScenarioNode.class);
    //******* Init *******
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    //******* Ports ******
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    //******* Children ******
    protected final Component overlay = create(ScenarioOverlay.class, Init.NONE);
    protected final Component boot;

    {

        Optional<NetAddress> serverO = config().readValue("id2203.project.bootstrap-address", NetAddress.class);
        if (serverO.isPresent()) { // start in client mode
            boot = create(BootstrapClient.class, Init.NONE);
        } else { // start in server mode
            boot = create(BootstrapServer.class, Init.NONE);
        }
        connect(timer, boot.getNegative(Timer.class), Channel.TWO_WAY);
        connect(net, boot.getNegative(Network.class), Channel.TWO_WAY);
        // Overlay
        connect(boot.getPositive(Bootstrapping.class), overlay.getNegative(Bootstrapping.class), Channel.TWO_WAY);
        connect(net, overlay.getNegative(Network.class), Channel.TWO_WAY);

    }

}
