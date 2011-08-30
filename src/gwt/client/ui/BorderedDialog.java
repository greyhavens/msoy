//
// $Id$

package client.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

/**
 * Displays a bordered popup with an area for a title and contents.
 */
public abstract class BorderedDialog extends BorderedPopup
{
    public BorderedDialog ()
    {
        this(false);
    }

    public BorderedDialog (boolean autoHide)
    {
        this(autoHide, false);
    }

    public BorderedDialog (boolean autoHide, boolean omitCloseBox)
    {
        this(autoHide, omitCloseBox, true);
    }

    public BorderedDialog (boolean autoHide, boolean omitCloseBox, boolean enableDrag)
    {
        super(autoHide);

        _main = new SmartTable("borderedDialog", 0, 0);
        _main.setText(0, 0, "");
        if (!omitCloseBox) {
            _main.setWidget(0, 1, MsoyUI.createCloseButton(new ClickHandler() {
                public void onClick (ClickEvent event) {
                    hide();
                }
            }), 1, "CloseCell");
        }
        setWidget(_main);
    }

    public void setHeaderTitle (String text)
    {
        Label label = MsoyUI.createLabel(text, "HeaderTitle");
        DragHandler dragHandler = new DragHandler();
        label.addMouseDownHandler(dragHandler);
        label.addMouseUpHandler(dragHandler);
        label.addMouseMoveHandler(dragHandler);
        _main.setWidget(0, 0, label);
    }

    public void setContents (Widget contents)
    {
        _main.setWidget(1, 0, contents, _main.getCellCount(0));
    }

    public void addButton (Button button)
    {
        if (_buttons == null) {
            _buttons = new HorizontalPanel();
            _main.setWidget(2, 0, _buttons, _main.getCellCount(0), "Buttons");
            _main.getFlexCellFormatter().setHorizontalAlignment(2, 0, HasAlignment.ALIGN_RIGHT);
        }
        if (_buttons.getWidgetCount() > 0) {
            _buttons.add(WidgetUtil.makeShim(5, 5));
        }
        _buttons.add(button);
    }

    /**
     * Creates a click listener that executes the supplied command and then hides this dialog.
     */
    public ClickHandler onAction (final Command command)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                if (command != null) {
                    command.execute();
                }
                hide();
            }
        };
    }

    /**
     * Creates a click listener that simply hides this dialog. Useful for cancel buttons.
     */
    public ClickHandler onCancel ()
    {
        return onAction(null);
    }

    /** Creates the drag listener. */
    protected class DragHandler
        implements MouseDownHandler, MouseUpHandler, MouseMoveHandler {
        public void onMouseDown (MouseDownEvent event) {
            _dragging = true;
            DOM.setCapture(((Widget)event.getSource()).getElement());
            _dragStartX = event.getX();
            _dragStartY = event.getY();
        }

        public void onMouseMove (MouseMoveEvent event) {
            if (_dragging) {
                int absX = event.getX() + getAbsoluteLeft();
                int absY = event.getY() + getAbsoluteTop();
                setPopupPosition(absX - _dragStartX, absY - _dragStartY);
                updateFrame();
            }
        }

        public void onMouseUp (MouseUpEvent event) {
            _dragging = false;
            DOM.releaseCapture(((Widget)event.getSource()).getElement());
        }

        protected boolean _dragging;
        protected int _dragStartX, _dragStartY;
    }

    protected SmartTable _main;
    protected HorizontalPanel _buttons;
}
