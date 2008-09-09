//
// $Id$

package client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
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
            _main.setWidget(0, 1, MsoyUI.createCloseButton(new ClickListener() {
                public void onClick (Widget sender) {
                    hide();
                }
            }), 1, "CloseCell");
        }
        setWidget(_main);
    }

    public void setHeaderTitle (String text)
    {
        Label label = MsoyUI.createLabel(text, "HeaderTitle");
        label.addMouseListener(createDragListener());
        _main.setWidget(0, 0, label);
    }

    public void setContents (Widget contents)
    {
        _main.setWidget(1, 0, contents, _main.getCellCount(0), null);
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
     * Creates a click listener that simply hides this dialog. Useful for cancel buttons.
     */
    public ClickListener onCancel ()
    {
        return new ClickListener() {
            public void onClick (Widget sender) {
                hide();
            }
        };
    }

    /** Creates the drag listener. */
    protected MouseListenerAdapter createDragListener () {
        return new MouseListenerAdapter() {
            public void onMouseDown (Widget sender, int x, int y) {
                _dragging = true;
                DOM.setCapture(sender.getElement());
                _dragStartX = x;
                _dragStartY = y;
            }

            public void onMouseMove (Widget sender, int x, int y) {
                if (_dragging) {
                    int absX = x + getAbsoluteLeft();
                    int absY = y + getAbsoluteTop();
                    setPopupPosition(absX - _dragStartX, absY - _dragStartY);
                    updateFrame();
                }
            }

            public void onMouseUp (Widget sender, int x, int y) {
                _dragging = false;
                DOM.releaseCapture(sender.getElement());
            }

            protected boolean _dragging;
            protected int _dragStartX, _dragStartY;
        };
    }

    protected SmartTable _main;
    protected HorizontalPanel _buttons;
}
