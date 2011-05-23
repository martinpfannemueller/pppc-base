package info.pppc.base.swtui.console;

import info.pppc.base.swtui.AbstractControl;
import info.pppc.base.system.event.IListener;

import java.io.InputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

/**
 * The console control that is used to display a console in the bottom of the
 * gui. The console redirects the output stream used for base logging into a
 * nice and stylish text window.
 * 
 * @author Marcus Handte
 */
public class ConsoleControl extends AbstractControl {

	/**
	 * The content listener that listens to changes in the content provider.
	 * Whenever the content of the provider changes, the listener will force
	 * an update.
	 */
	private IListener content = new IListener() {
		public void handleEvent(info.pppc.base.system.event.Event event) {
			update();
		}
	};

	/**
	 * The text viewer used to display the console document.
	 */
	private Text viewer;

	/**
	 * The document that contains the contents of the text viewer.
	 */
	private ConsoleContentProvider provider;

	/**
	 * The scrolled composite that contains the text viewer and adds
	 * the scrollbars.
	 */
	private ScrolledComposite composite;

	/**
	 * Creates a new console control that reads its data
	 * from the specified input stream.
	 * 
	 * @param input The input stream that provides the output
	 * 	data.
	 */
	public ConsoleControl(InputStream input) {
		provider = new ConsoleContentProvider(input);
		Thread t = new Thread(provider);
		t.start();
	}

	/**
	 * Displays the console control on the specified composite.
	 * 
	 * @param parent The parent composite used to display the 
	 * 	console.
	 */
	public synchronized void showControl(Composite parent) {
		composite = new ScrolledComposite(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		composite.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				update();
			}
		});
		viewer = new Text(composite, SWT.READ_ONLY | SWT.MULTI);
		composite.setContent(viewer);
		setControl(composite);
		provider.addContentListener
			(ConsoleContentProvider.EVENT_CONTENT_CHANGED, content);
		viewer.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
		composite.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
		update();
	}

	/**
	 * Called whenever the console is disposed by the application. For the
	 * console control this occurs only if the application window is closed.
	 */
	public synchronized void disposeControl() {
		provider.removeContentListener
			(ConsoleContentProvider.EVENT_CONTENT_CHANGED, content);
		viewer = null;
		composite = null;
		super.disposeControl();
	}

	/**
	 * Adjusts the size of the text viewer in such a way that the scroll
	 * pane is always filled. This method is called upon every resize of
	 * the scroll pane and upon every text change of the document that
	 * is currently displayed by the text viewer.
	 */
	private void update() {
		Display d = getDisplay();
		if (d != null) {
			d.asyncExec(new Runnable() {
				public void run() {
					if (viewer != null && composite != null) {
						viewer.setText(provider.getContent());
						Point p1 = viewer.computeSize(SWT.DEFAULT, SWT.DEFAULT);
						Rectangle r = ((Composite)getControl()).getClientArea();
						Point p2 = new Point(r.width, r.height);
						Point p = new Point(p2.x, p2.y);
						p.x = Math.max(p.x, p1.x);
						p.y = Math.max(p.y, p1.y);
						viewer.setSize(p);
					}						
				}
			});			
		}
	}

}
