//
// $Id$

package client.ui;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Wraps a StretchPanel and provides it with dependent styles for button states.
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

        MouseHandler handler = new MouseHandler();
        addMouseOverHandler(handler);
        addMouseOutHandler(handler);
        addMouseDownHandler(handler);
        addMouseUpHandler(handler);
    }

    public void setContent (Widget content)
    {
        _panel.setContent(content);
    }

    public Widget getContent ()
    {
        return _panel.getContent();
    }

    protected class MouseHandler
        implements MouseOverHandler, MouseOutHandler, MouseDownHandler, MouseUpHandler
    {
        public void onMouseOver (MouseOverEvent event) {
            _panel.addStyleDependentName("up-hovering");
        }
        public void onMouseOut (MouseOutEvent event) {
            _panel.removeStyleDependentName("up-hovering");
            _panel.removeStyleDependentName("down-hovering");
        }
        public void onMouseDown (MouseDownEvent event) {
            _panel.addStyleDependentName("down-hovering");
        }
        public void onMouseUp (MouseUpEvent event) {
            _panel.removeStyleDependentName("down-hovering");
        }
    }

    protected StretchPanel _panel;
}
