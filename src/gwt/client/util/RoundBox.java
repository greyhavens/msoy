//
// $Id$

package client.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import client.util.images.box.BoxImages;

/**
 * Basically a {@link VerticalPanel} with a rounded background. It's not actually a VerticalPanel
 * so don't get any funny ideas.
 */
public class RoundBox extends SmartTable
{
    public static final String WHITE = "White";

    public static final String BLUE = "Blue";

    /**
     * Creates a round box in either {@link #WHITE} or {@link #BLUE}.
     */
    public RoundBox (String color)
    {
        super("roundBox", 0, 0);
        addStyleName("roundBox" + color);

        if (color.equals(WHITE)) {
            // TODO
        } else if (color.equals(BLUE)) {
            setWidget(0, 0, _images.blue_upper_left().createImage(), 1, "Corner");
            setWidget(0, 2, _images.blue_upper_right().createImage(), 1, "Corner");
            setWidget(2, 0, _images.blue_lower_left().createImage(), 1, "Corner");
            setWidget(2, 1, _images.blue_lower_right().createImage(), 1, "Corner");
        }

        setWidget(0, 1, _contents = new FlowPanel(), 1, "Contents");
        getFlexCellFormatter().setRowSpan(0, 1, 3);

        setHTML(1, 0, "&nbsp;");
        getFlexCellFormatter().setStyleName(1, 0, "Side");
        setHTML(1, 1, "&nbsp;");
        getFlexCellFormatter().setStyleName(1, 1, "Side");
    }

    public void add (Widget widget)
    {
        _contents.add(widget);
    }

    public int getWidgetCount ()
    {
        return _contents.getWidgetCount();
    }

    protected FlowPanel _contents;

    /** Our navigation menu images. */
    protected static BoxImages _images = (BoxImages)GWT.create(BoxImages.class);
}
