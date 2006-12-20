//
// $Id$

package client.util;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.PopupListener;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Displays a popup with a nice border around it.
 */
public class BorderedPopup extends PopupPanel
{
    public BorderedPopup ()
    {
        this(false);
    }

    public BorderedPopup (boolean autoHide)
    {
        super(autoHide);
        setStyleName("borderedPopup");

        super.setWidget(_outer = new FlexTable());
        DOM.setAttribute(_outer.getElement(), "cellSpacing", "0");
        DOM.setAttribute(_outer.getElement(), "cellPadding", "0");
        _outer.getFlexCellFormatter().setStyleName(0, 0, "UpperLeft");
        _outer.getFlexCellFormatter().setStyleName(0, 1, "Upper");
        _outer.getFlexCellFormatter().setStyleName(0, 2, "UpperRight");
        _outer.getFlexCellFormatter().setStyleName(1, 0, "Left");
        _outer.getFlexCellFormatter().setStyleName(1, 2, "Right");
        _outer.getFlexCellFormatter().setStyleName(2, 0, "LowerLeft");
        _outer.getFlexCellFormatter().setStyleName(2, 1, "Lower");
        _outer.getFlexCellFormatter().setStyleName(2, 2, "LowerRight");

        // listen for our own closes and export that in a handy calldown method
        addPopupListener(new PopupListener() {
            public void onPopupClosed (PopupPanel panel, boolean autoClosed) {
                onClosed(autoClosed);
            }
        });
    }

    // @Override // from SimplePanel
    public void setWidget (Widget contents)
    {
        _outer.setWidget(1, 1, contents);
    }

    // @Override // from PopupPanel
    public void show ()
    {
        // start off screen so that we are not visible until we can compute our proper location and
        // center ourselves; we'd call setPopupPosition() but that foolishly bounds our position to
        // greater than zero, to protect us from ourselves no doubt
        Element elem = getElement();
        DOM.setStyleAttribute(elem, "left", "-5000px");
        DOM.setStyleAttribute(elem, "top", "-5000px");
        super.show();
        recenter();
    }

    /**
     * Recenters our popup.
     */
    protected void recenter ()
    {
        setPopupPosition((Window.getClientWidth() - getOffsetWidth()) / 2,
                         (Window.getClientHeight() - getOffsetHeight()) / 2);
    }

    /**
     * Called when this popup is dismissed.
     */
    protected void onClosed (boolean autoClosed)
    {
    }

    protected FlexTable _outer;
}
