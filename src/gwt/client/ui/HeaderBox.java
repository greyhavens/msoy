//
// $Id$

package client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import client.images.box.BoxImages;

/**
 * A box with a curvy header.
 */
public class HeaderBox extends FlowPanel
{
    public HeaderBox (String iconPath, String title)
    {
        setStyleName("headerBox");

        SmartTable header = new SmartTable(0, 0);
        header.setWidth("100%");
        header.setWidget(0, 0, new Image("/images/ui/box/header_left.png"), 1, "Corner");
        if (iconPath != null) {
            FlowPanel tbox = new FlowPanel();
            tbox.add(new Image(iconPath));
            tbox.add(new Label(title));
            header.setWidget(0, 1, tbox, 1, "Title");
        } else {
            header.setText(0, 1, title, 1, "Title");
        }
        header.getFlexCellFormatter().setHorizontalAlignment(0, 1, HasAlignment.ALIGN_CENTER);
        header.setWidget(0, 2, new Image("/images/ui/box/header_right.png"), 1, "Corner");
        super.add(header);

        _contents = new FlowPanel();
        _contents.setStyleName("Contents");
        super.add(_contents);
    }

    public HeaderBox (String title, Widget contents)
    {
        this(null, title);
        add(contents);
    }

    public HeaderBox (String iconPath, String title, Widget contents)
    {
        this(iconPath, title);
        add(contents);
    }

    public HeaderBox makeRoundBottom ()
    {
        SmartTable footer = new SmartTable(0, 0);
        footer.setWidth("100%");
        footer.setWidget(0, 0, _images.white_lower_left().createImage());
        footer.getFlexCellFormatter().setStyleName(0, 1, "Contents");
        footer.getFlexCellFormatter().setWidth(0, 1, "100%");
        footer.setWidget(0, 2, _images.white_lower_right().createImage());
        super.add(footer);
        return this;
    }

    @Override // from Panel
    public void add (Widget widget)
    {
        _contents.add(widget);
    }

    protected FlowPanel _contents;

    /** Our corner images. */
    protected static BoxImages _images = (BoxImages)GWT.create(BoxImages.class);
}
