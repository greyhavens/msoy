//
// $Id$

package client.ui;


import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;

/**
 * Displays a popup informational message.
 */
public class InfoPopup extends BorderedPopup
{
    public InfoPopup (String message)
    {
        super(true);
        _autoClearTimeout = Math.max(MIN_AUTO_CLEAR_DELAY, message.length() * PER_CHAR_CLEAR_DELAY);
        setWidget(MsoyUI.createLabel(message, "infoPopup"));
    }

    public InfoPopup (Widget contents)
    {
        super(true);
        contents.addStyleName("infoPopup");
        setWidget(contents);
    }

    public void showNear (Widget parent)
    {
        _centerOnShow = false;
        setPopupPosition(parent.getAbsoluteLeft(),
                         parent.getAbsoluteTop() + parent.getOffsetHeight() + NEAR_GAP);
        show();
    }

    protected void onAttach ()
    {
        super.onAttach();
        Timer autoClear = new Timer() {
            public void run () {
                hide();
            }
        };
        autoClear.schedule(_autoClearTimeout);
    }

    // we use an int here because that's what Timer wants; whee!
    protected int _autoClearTimeout = DEFAULT_AUTO_CLEAR_DELAY;

    protected static final int MIN_AUTO_CLEAR_DELAY = 3000;
    protected static final int DEFAULT_AUTO_CLEAR_DELAY = 5000;
    protected static final int PER_CHAR_CLEAR_DELAY = 50;

    protected static final int NEAR_GAP = 5;
}
