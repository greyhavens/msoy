//
// $Id$

package client.ui;

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

        addMouseListener(new MouseListenerAdapter() {
            @Override public void onMouseEnter (Widget sender) {
                _panel.addStyleDependentName("up-hovering");
            }
            @Override public void onMouseLeave (Widget sender) {
                _panel.removeStyleDependentName("up-hovering");
                _panel.removeStyleDependentName("down-hovering");
            }
            @Override public void onMouseDown (Widget sender, int x, int y) {
                _panel.addStyleDependentName("down-hovering");
            }
            @Override public void onMouseUp (Widget sender, int x, int y) {
                _panel.removeStyleDependentName("down-hovering");
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
