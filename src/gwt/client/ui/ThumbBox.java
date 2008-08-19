//
// $Id$

package client.ui;

import client.util.MediaUtil;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.SimplePanel;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Displays an optionally clickable thumbnail image.
 */
public class ThumbBox extends SimplePanel
{
    public ThumbBox (MediaDesc desc, ClickListener onClick)
    {
        this(desc, MediaDesc.THUMBNAIL_SIZE, onClick);
    }

    public ThumbBox (MediaDesc desc, int size, ClickListener onClick)
    {
        this(desc, MediaDesc.getWidth(size), MediaDesc.getHeight(size), onClick);
    }

    public ThumbBox (MediaDesc desc, int width, int height, ClickListener onClick)
    {
        addStyleName("thumbBox");
        setWidth(width + "px");
        setHeight(height + "px");
        DOM.setStyleAttribute(getElement(), "overflow", "hidden");
        add(MediaUtil.createMediaView(desc, width, height, onClick));
    }
}
