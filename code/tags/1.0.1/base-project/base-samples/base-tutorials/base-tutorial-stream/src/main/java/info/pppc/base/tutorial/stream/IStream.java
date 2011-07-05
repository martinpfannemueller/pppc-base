package info.pppc.base.tutorial.stream;

import info.pppc.base.system.IStreamHandler;

/**
 * The interface of the stream tutorial service.
 * It extends the stream handler marker interface.
 * By doing so, the Eclipe plug-in with the proxy 
 * generator  will generate appropriate connect methods.
 * 
 * @author Marcus Handte
 */
public interface IStream extends IStreamHandler {

}
