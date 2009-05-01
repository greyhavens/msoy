//
// $Id$

package client.ui;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Wraps a StretchPanel and provides it with dependant styles for button states.
 */
public class StretchButton extends FocusPanel
{
    public static final String ORANGE_THICK = "orangeThick";
    public static final String BLUE_THICK = "blueThick";
    public static final String BLUE_THIN = "blueThin";

    public static StretchButton makeOrange (String label, ClickHandler listener)
    {
        return new StretchButton(ORANGE_THICK, label, listener);
    }

    public static StretchButton makeBlue (String label, ClickHandler listener)
    {
        return new StretchButton(BLUE_THICK, label, listener);
    }

    public StretchButton (String styleName, String textContent, ClickHandler listener)
    {
        // create a content label with the appropriate styling
        this(styleName, new Label(textContent));

        if (listener != null) {
            addClickHandler(listener);
        }
    }

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
