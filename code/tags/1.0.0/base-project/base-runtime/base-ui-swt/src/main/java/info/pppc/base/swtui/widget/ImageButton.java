package info.pppc.base.swtui.widget;

import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * The image button is a drop in replacement for non-working image buttons on ipaqs.
 * This is just a quick and dirty hack. However, it should suffice.
 * 
 * @author Marcus Handte
 */
public class ImageButton extends Canvas implements Listener {
	
	/**
	 * The size of the border. This is the spacing between
	 * the border and the image.
	 */
	private static final int BORDER_SIZE = 5;
	
	/**
	 * The selection listeners that have been registered.
	 */
	private Vector listeners = new Vector();
	
	/**
	 * The image shown if the button is enabled.
	 */
	private Image enabledImage;
	
	/**
	 * The image shown if the button is disabled.
	 */
	private Image disabledImage;

	/**
	 * The state of the button. This will be true if the
	 * button has been pressed but no released so far.
	 */
	private boolean pressed = false;
	
	/**
	 * The state of the button. This will be true if the
	 * button has been pressed and the mouse is still 
	 * in the client area of the button.
	 */
	private boolean armed = false;
	
	/**
	 * The color used to draw the disabled image. This
	 * will be initialized to the systems default color
	 * gray.
	 */
	private Color disabledColor;
	
	/**
	 * The client area that is maintained and cached 
	 * whenever the control is resized.
	 */
	private Rectangle area;
	
	/**
	 * A flag that indicates whether the button is enabled.
	 */
	private boolean on = true;
	
	/**
	 * Creates a new image button using the specified parent and
	 * style.
	 * 
	 * @param parent The parent of the button.
	 * @param style The style of the button.
	 */
	public ImageButton(Composite parent, int style) {
		super(parent, style);
		addListener(SWT.Paint, this);
		addListener(SWT.Resize, this);
		addListener(SWT.MouseDown, this);
		addListener(SWT.MouseUp, this);
		addListener(SWT.MouseMove, this);
		addListener(SWT.FocusIn, this);
		addListener(SWT.FocusOut, this);
		addListener(SWT.KeyDown, this);
		addListener(SWT.Traverse, this);
		disabledColor = getDisplay().getSystemColor(SWT.COLOR_GRAY);
		setBackground(parent.getBackground());
		area = getClientArea();
	}

	/**
	 * Adds a selection listener to the button. The listener
	 * will be notified if the button is pressed. This will
	 * never happen if the button is disabled.
	 * 
	 * @param listener The listener to add.
	 */
	public void addSelectionListener(SelectionListener listener) {
		if (listener != null) listeners.addElement(listener);
	}

	/**
	 * Removes a previously registered listener from the button.
	 * 
	 * @param listener The listener to remove.
	 */
	public void removeSelectionListener (SelectionListener listener) {
		if (listeners != null) listeners.removeElement(listener);
	}
	
	/**
	 * Sends a selection event to all registered listeners.
	 * 
	 * @param e The event to send.
	 */
	private void fireSelection(Event e) {
		for (int i = 0; i < listeners.size(); i++) {
			SelectionListener listener = (SelectionListener)listeners.elementAt(i);
			listener.widgetSelected(new SelectionEvent(e));
		}	
	}
	
	/**
	 * Draws the button on the given graphic context.
	 * 
	 * @param gc The graphic context to draw on.
	 */
	private void paint(GC gc) {
		Image draw = on?enabledImage:disabledImage;
		if (draw != null) {
			Rectangle d = draw.getBounds();
			Rectangle c = area;
			setForeground(on?null:disabledColor);
			gc.drawRectangle(c.x, c.y, c.width - 1, c.height - 1);
			int offset = 0;
			if (armed) offset = 1;
			gc.drawImage (draw, 0, 0, d.width, d.height, 
				BORDER_SIZE + offset, BORDER_SIZE + offset, c.width - BORDER_SIZE * 2, 
					c.height - BORDER_SIZE * 2);
			if (isFocusControl()) {
				gc.drawFocus(c.x + 2, c.y + 2, c.width - 4, c.height - 4);	
			}	
		}
	}
	
	/**
	 * Checks whether the coordinate of the event is contained in the
	 * current client area.
	 * 
	 * @param event The event that provides the mouse coordinates.
	 * @return True if the mouse is inside the button, false otherwise.
	 */
	private boolean contains(Event event) {
		return event.x >= area.x && event.x < area.x+area.width &&
			event.y >= area.y && event.y < area.y+area.height;
	}
	
	/**
	 * Sets the image that will be shown by the button. If the image
	 * is null, no image will be shown. Note that the image must be
	 * released by the calling client. It will not be released by
	 * this control as it did not create it.
	 * 
	 * @param image The image of the button.
	 */
	public void setImage(Image image) {
		checkWidget();
		if (disabledImage != null) {
			disabledImage.dispose();
		}
		this.enabledImage = image;
		if (enabledImage != null) {
			ImageData mask = enabledImage.getImageData().getTransparencyMask();
			PaletteData paletteData = new PaletteData(
			        new RGB[] { getBackground().getRGB(), disabledColor.getRGB()});
			ImageData data = new ImageData(mask.width, mask.height, 1, paletteData, 1, mask.data);
			disabledImage = new Image(getDisplay(), data);
		}
	}
	
	/**
	 * Returns the current image or null if none is set.
	 * 
	 * @return The current image or null.
	 */
	public Image getImage() {
		checkWidget();
		return enabledImage;
	}
	
	/**
	 * Deals with the specified event. This method must not be
	 * called from outside the class.
	 * 
	 * @param event The event to deal with.
	 */
	public void handleEvent(Event event) {
		switch (event.type) {
			case SWT.Paint:
				paint(event.gc);
				break;
			case SWT.MouseDown:
				armed = true;
				pressed = true;
				redraw();
				break;
			case SWT.MouseUp:
				if (pressed && armed) {
					redraw();
					fireSelection(event);
					armed = false;
				}
				pressed = false;
				break;
			case SWT.MouseMove:
				if (pressed) {
					boolean in = contains(event);
					if (in & ! armed) {
						armed = true;
						redraw();
					} else if (! in & armed) {
						armed = false;
						redraw();
					}
				}
				break;
			case SWT.FocusIn:
				redraw();
				break;
			case SWT.FocusOut:
				redraw();
				break;
			case SWT.KeyDown:
				if(event.character == ' ') {
					fireSelection(event);
				}
				break;
			case SWT.Resize:
				area = getClientArea();
				break;
			case SWT.Traverse:
				if (event.detail == SWT.TRAVERSE_RETURN) {
					fireSelection(event);
				}
				event.doit = true;
				break;
		}
	};
	
	/**
	 * Enables or disables the button.
	 * 
	 * @param enabled True to enable, false to disable.
	 */
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (on != enabled) {
			on = enabled;
			redraw();
		}
	}

	/**
	 * Computes the width and height of the button. The dimensions
	 * will depend on the size of the image. 
	 * 
	 * @param wHint The hint for the width.
	 * @param hHint The hint for the height.
	 * @param changed Use cached information
	 * @return The width and height of the button. 
	 */
	public Point computeSize(int wHint, int hHint, boolean changed) {
		if (enabledImage != null) {
			Rectangle d = enabledImage.getBounds();
			return new Point(d.width - d.x + BORDER_SIZE * 2, d.height - d.y + BORDER_SIZE * 2);
		} else {
			return super.computeSize(wHint, hHint, changed);	
		}
	}
	
	/**
	 * Disposes the button and releases all internal state.
	 */
	public void dispose() {
		super.dispose();
		disabledColor = null;
		if (disabledImage != null) {
			disabledImage.dispose();
		}
	}
	
	/**
	 * Determines whether the button can have children. Guess what: no.
	 * 
	 * @return Always false as the button does not support children.
	 */
	public boolean isReparentable () {
	    checkWidget ();
	    return false;
	}
	
}
