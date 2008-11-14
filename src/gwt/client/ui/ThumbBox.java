//
// $Id$

package client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.SimplePanel;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.web.gwt.Pages;

import client.util.Link;
import client.util.MediaUtil;

/**
 * Displays an optionally clickable thumbnail image.
 */
public class ThumbBox extends SimplePanel
{
    public ThumbBox (MediaDesc desc)
    {
        this(desc, MediaDesc.THUMBNAIL_SIZE);
    }

    public ThumbBox (MediaDesc desc, int size)
    {
        this(desc, MediaDesc.getWidth(size), MediaDesc.getHeight(size), null);
    }

    public ThumbBox (MediaDesc desc, Pages page, String args)
    {
        this(desc, MediaDesc.THUMBNAIL_SIZE, page, args);
    }

    public ThumbBox (MediaDesc desc, int size, Pages page, String args)
    {
        this(desc, MediaDesc.getWidth(size), MediaDesc.getHeight(size),
             Link.createListener(page, args));
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
