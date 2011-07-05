package info.pppc.base.system.plugin;

/**
 * The interface of the plug-in manager that is exposed to routing plug-ins.
 * It consists of the discovery interface for route announcements, the
 * semantic interface for dispatching and the transceiver interface for
 * accepting incoming connection requests.
 * 
 * @author Marcus Handte
 */
public interface IRoutingManager extends IDiscoveryManager, ISemanticManager, ITransceiverManager {

}
