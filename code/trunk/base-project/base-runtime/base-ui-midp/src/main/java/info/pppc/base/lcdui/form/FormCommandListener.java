package info.pppc.base.lcdui.form;

import javax.microedition.lcdui.Command;

/**
 * The command listener is a drop in replacement of the
 * item command listener.
 * 
 * @author Marcus Handte
 */
public interface FormCommandListener {

	/**
	 * Called whenever the specified command is fired on 
	 * the specified item.
	 * 
	 * @param command The command that has been fired.
	 * @param item The item that has caused the command.
	 */
	public void commandAction(Command command, FormItem item);
	
}
