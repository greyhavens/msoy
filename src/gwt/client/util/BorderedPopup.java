//
// $Id$

package client.util;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlexTable;
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
    }

    // @Override // from SimplePanel
    public void setWidget (Widget contents)
    {
        _outer.setWidget(1, 1, contents);
    }

    // @Override // from PopupPanel
    public void show ()
    {
        // start somewhere that we won't booch the window size when we first open
        setPopupPosition(0, Window.getClientHeight() / 2);
        super.show();
        recenter(false);
    }

    /**
     * Recenters our popup.
     */
    protected void recenter (boolean defer)
    {
        if (defer) {
            DeferredCommand.add(new Command() {
                public void execute () {
                    recenter(false);
                }
            });
        } else {
            setPopupPosition((Window.getClientWidth() - getOffsetWidth()) / 2,
                             (Window.getClientHeight() - getOffsetHeight()) / 2);
        }
    }

    protected FlexTable _outer;
}
