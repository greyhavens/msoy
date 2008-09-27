//
// $Id$

package client.ui;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Wraps a StretchPanel and provides it with dependant styles for button states.
 */
public class StretchButton extends FocusPanel
{
    public StretchButton (String styleName, Widget content)
    {
        setWidget(_panel = new StretchPanel(styleName, content));
        setStyleName("stretchButton");

        // TODO: Button down states, but always "up" for now
        _panel.addStyleDependentName("up");

        addMouseListener(new MouseListenerAdapter() {
            public void onMouseEnter (Widget sender) {
                _panel.addStyleDependentName("up-hovering");
            }
            public void onMouseLeave (Widget sender) {
                _panel.removeStyleDependentName("up-hovering");
            }
        });
    }

    public void setContent (Widget content)
    {
        _panel.setContent(content);
    }

    public Widget getContent ()
    {
        return _panel.getContent();
    }

    protected StretchPanel _panel;
}
