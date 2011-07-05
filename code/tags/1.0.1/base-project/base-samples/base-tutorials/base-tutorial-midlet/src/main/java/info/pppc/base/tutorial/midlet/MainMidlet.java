package info.pppc.base.tutorial.midlet;

import info.pppc.base.lcdui.Application;
import info.pppc.base.lcdui.action.GarbageAction;
import info.pppc.base.lcdui.action.SystemAction;
import info.pppc.base.system.InvocationBroker;
import info.pppc.base.system.PluginManager;
import info.pppc.basex.plugin.discovery.ProactiveDiscovery;
import info.pppc.basex.plugin.semantic.RmiSemantic;
import info.pppc.basex.plugin.serializer.ObjectSerializer;
import info.pppc.basex.plugin.transceiver.MxBluetoothTransceiver;
import info.pppc.basex.plugin.transceiver.bt.TimedDiscoveryStrategy;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * This class demonstrates how to use BASE within a midlet. It simply 
 * start the midlet gui and it loads a number of plug-ins. Note that
 * this midlet can be started multiple times in the J2ME WTK3.0 to
 * test the bluetooth connectivity.
 * 
 * @author Mac
 */
public class MainMidlet extends MIDlet {

	/**
	 * Instantiates the midlet.
	 */
	public MainMidlet() { }

	/**
	 * Called to destroy the midlet. Usually, the
	 * vm will simply kill the jvm after this call
	 * so it is not really necessary to perform
	 * any cleanup here.
	 */
	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {	}

	/**
	 * Called to pause the midlet. I have not seen a
	 * phone so far that actually calls this method.
	 * Technically, it would make sense to "unload"
	 * all plug-ins in BASE in order to free the
	 * resources.
	 */
	protected void pauseApp() {	}

	/**
	 * This is the only method that must be implemented. It is
	 * called upon startup and it loads the ui, the broker and
	 * the plug-ins.
	 */
	protected void startApp() throws MIDletStateChangeException {
		// Load the midlet ui (this is a snap-in ui that replaces
		// the standard midlet ui with a "non-broken" version
		// I wrote it since the Nokia Series 60 has a broken gui
		final Application application = Application.getInstance(this);
		// install some standard actions into the main menu of the ui
		application.run(new Runnable() {
			public void run() {
				// this action enables the user to visualize the
				// configuration of the connected systems
				application.addAction(new SystemAction(application));
				// this action prints some statistics to the console
				application.addAction(new GarbageAction());		
			}
		});
		// now show the user interface - after this call,
		// all output is logged to the console, so it makes sense
		// to call this method before installing the plug-ins
		// this way you can see the output of the plug-ins in the
		// console of the phone.
		application.show();
		// get the reference to the broker and its plug-in manager
		InvocationBroker b = InvocationBroker.getInstance();
		PluginManager m = b.getPluginManager();
		// install a standard stack of plug-ins.
		m.addPlugin(new RmiSemantic());
		m.addPlugin(new ObjectSerializer());
		m.addPlugin(new ProactiveDiscovery());
		m.addPlugin(new MxBluetoothTransceiver(new TimedDiscoveryStrategy(60000), false));
	}

}
