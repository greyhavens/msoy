//
// $Id$

package client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import client.images.box.BoxImages;

/**
 * Basically a {@link VerticalPanel} with a rounded background. It's not actually a VerticalPanel
 * so don't get any funny ideas.
 */
public class RoundBox extends SmartTable
{
    public static final String WHITE = "White";
    public static final String BLUE = "Blue";
    public static final String DARK_BLUE = "DarkBlue";
    public static final String MEDIUM_BLUE = "MediumBlue";

    /**
     * Creates a round box in either {@link #WHITE} or {@link #BLUE}.
     */
    public RoundBox (String color)
    {
        super("roundBox", 0, 0);

        if (color.equals(WHITE)) {
            setWidget(0, 0, _images.white_upper_left().createImage(), 1, "Corner");
            setWidget(0, 2, _images.white_upper_right().createImage(), 1, "Corner");
            setWidget(2, 0, _images.white_lower_left().createImage(), 1, "Corner");
            setWidget(2, 2, _images.white_lower_right().createImage(), 1, "Corner");
        } else if (color.equals(BLUE)) {
            setWidget(0, 0, _images.blue_upper_left().createImage(), 1, "Corner");
            setWidget(0, 2, _images.blue_upper_right().createImage(), 1, "Corner");
            setWidget(2, 0, _images.blue_lower_left().createImage(), 1, "Corner");
            setWidget(2, 2, _images.blue_lower_right().createImage(), 1, "Corner");
        } else if (color.equals(DARK_BLUE)) {
            setWidget(0, 0, _images.darkblue_upper_left().createImage(), 1, "Corner");
            setWidget(0, 2, _images.darkblue_upper_right().createImage(), 1, "Corner");
            setWidget(2, 0, _images.darkblue_lower_left().createImage(), 1, "Corner");
            setWidget(2, 2, _images.darkblue_lower_right().createImage(), 1, "Corner");
        } else if (color.equals(MEDIUM_BLUE)) {
            setWidget(0, 0, _images.medblue_upper_left().createImage(), 1, "Corner");
            setWidget(0, 2, _images.medblue_upper_right().createImage(), 1, "Corner");
            setWidget(2, 0, _images.medblue_lower_left().createImage(), 1, "Corner");
            setWidget(2, 2, _images.medblue_lower_right().createImage(), 1, "Corner");
        }

        setWidget(1, 1, _contents = new FlowPanel(), 1, "Center");
        getFlexCellFormatter().addStyleName(1, 1, color + "Center");

        getFlexCellFormatter().setStyleName(1, 0, color + "Side");
        getFlexCellFormatter().setStyleName(1, 2, color + "Side");

        getFlexCellFormatter().setStyleName(0, 1, color + "Center");
        getFlexCellFormatter().setStyleName(2, 1, color + "Center");
    }

    public void setHorizontalAlignment (
        HasHorizontalAlignment.HorizontalAlignmentConstant horizAlign)
    {
        getFlexCellFormatter().setHorizontalAlignment(1, 1, horizAlign);
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

    /** Our corner images. */
    protected static BoxImages _images = (BoxImages)GWT.create(BoxImages.class);
}
