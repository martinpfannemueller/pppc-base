package info.pppc.basex.plugin.routing;

import info.pppc.base.system.operation.IMonitor;
import info.pppc.base.system.operation.IOperation;
import info.pppc.base.system.operation.NullMonitor;
import info.pppc.base.system.plugin.IStreamConnector;
import info.pppc.base.system.util.Logging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The forwarding operation forwards data from one stream to another.
 * 
 * @author Marcus Handte
 */
public class RoutingOperation implements IOperation {

	/**
	 * The amount of buffer allocated for forwarding a single flow.
	 */
	public static final int FORWARD_BUFFER = 1024;
	
	/**
	 * The remote monitor to determine when to close the stream connector.
	 */
	IMonitor rmon;
	/**
	 * The input connector to read from.
	 */
	IStreamConnector cin;
	/**
	 * The output connector to write to.
	 */
	IStreamConnector cout;
	
	/**
	 * Creates a new forwarding operation that synchronized with closing the
	 * input connector onto the passed monitor.
	 * 
	 * @param cin The input connector to read from.
	 * @param cout The output connector to write to.
	 * @param monitor The monitor to wait for until closing.
	 */
	public RoutingOperation(IStreamConnector cin, IStreamConnector cout, NullMonitor monitor) {
		this.cout = cout;
		this.cin = cin;
		this.rmon = monitor;
	}
	
	/**
	 * Reads from the input and writes to the output until the 
	 * input is closed or the operation is canceled.
	 * 
	 * @param monitor The monitor to wait for.
	 */
	public void perform(IMonitor monitor) {
		InputStream input = null;
		OutputStream output = null;
		try {
			byte[] buffer = new byte[FORWARD_BUFFER];
			input = cin.getInputStream();
			output = cout.getOutputStream();
			while (! monitor.isCanceled()) {
				int result = input.read(buffer, 0, buffer.length);
				if (result == -1) {
					output.flush();
					break;
				} else {
					output.write(buffer, 0, result);
					output.flush();
				}
			}
		} catch (IOException e) {
			Logging.debug(getClass(), "Caught exception while forwarding data.");
		}
		try {
			if (input != null) input.close();				
		} catch (IOException ex) {
			Logging.debug(getClass(), "Could not close input stream.");
		}
		try {
			if (output != null) output.close();
		} catch (IOException ex) {
			Logging.debug(getClass(), "Could not close output stream.");
		}
		cout.release();
		cin.release();
		// cleanup the connection
		monitor.done();
		synchronized (rmon) {
			while (! rmon.isDone()) {
				try {
					rmon.join();	
				} catch (InterruptedException e) {
					Logging.error(getClass(), "Thread got interrupted.", e);
				}
			}
		}
	}
}