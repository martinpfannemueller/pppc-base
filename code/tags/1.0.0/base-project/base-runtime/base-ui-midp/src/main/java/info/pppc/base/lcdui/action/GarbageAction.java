package info.pppc.base.lcdui.action;

import info.pppc.base.lcdui.BaseUI;
import info.pppc.base.lcdui.element.ElementAction;
import info.pppc.base.system.util.Logging;

/**
 * The garbage action prints a memory report, runs the
 * garbage collector and prints a second memory report.
 * The report contains the free, used and total memory
 * of the jvm.
 * 
 * @author Marcus Handte
 */
public class GarbageAction extends ElementAction {

	/**
	 * The resource key for the externalized action name.
	 */
	private static final String UI_TEXT = "info.pppc.base.lcdui.action.GarbageAction.TEXT";
	
	/**
	 * Creates a new garbage action.
	 */
	public GarbageAction() {
		super(BaseUI.getText(UI_TEXT));
	}
	
	/**
	 * Executes the garbage action.
	 */
	public void run() {
		Logging.log(getClass(), "Executing cleanup action.");
		logMemory();
		Logging.log(getClass(), "Running garbage collector.");
		System.gc();
		logMemory();
		Logging.log(getClass(), "Cleanup action executed.");
	}
	
	/**
	 * Logs a memory report using base logging.
	 */
	private void logMemory() {
		long free = Runtime.getRuntime().freeMemory();
		long total = Runtime.getRuntime().totalMemory();
		long used = total - free;
		String freeString = getString(free, 8);
		String totalString = getString(total, 8);
		String usedString = getString(used, 8);
		StringBuffer b = new StringBuffer();
		b.append("MEMORY REPORT: USED(");
		b.append(usedString);
		b.append(") TOTAL (");
		b.append(totalString);
		b.append(") FREE (");
		b.append(freeString);
		b.append(").");
		Logging.log(getClass(), b.toString());
	}
	
	/**
	 * Returns a pretty printed long.
	 * 
	 * @param number The long to pretty print.
	 * @param length The length of the long (minimum).
	 * @return The pretty printed long.
	 */
	private String getString(long number, int length) {
		String s = Long.toString(number);
		while (s.length() < length) {
			s = " " + s;
		}
		return s;
	}
}
