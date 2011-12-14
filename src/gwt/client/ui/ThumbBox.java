//
// $Id$

package client.ui;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.SimplePanel;

import com.threerings.orth.data.MediaDesc;
import com.threerings.orth.data.MediaDescSize;

import com.threerings.msoy.web.gwt.MemberCard;
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
        this(desc, MediaDescSize.THUMBNAIL_SIZE);
    }

    public ThumbBox (MediaDesc desc, int size)
    {
        this(desc, MediaDescSize.getWidth(size), MediaDescSize.getHeight(size), null);
    }

    public ThumbBox (MediaDesc desc, Pages page, Object... args)
    {
        this(desc, MediaDescSize.THUMBNAIL_SIZE, page, args);
    }

    public ThumbBox (MediaDesc desc, int size, Pages page, Object... args)
    {
        this(desc, MediaDescSize.getWidth(size), MediaDescSize.getHeight(size),
             Link.createHandler(page, args));
    }

    public ThumbBox (MediaDesc desc, int width, int height, ClickHandler onClick)
    {
        addStyleName("thumbBox");
        setWidth(width + "px");
        setHeight(height + "px");
        DOM.setStyleAttribute(getElement(), "overflow", "hidden");
        add(MediaUtil.createMediaView(desc, width, height, onClick));
    }

    public static ThumbBox fromCard (MemberCard card, int size)
    {
        return new ThumbBox(card.photo, size, Pages.PEOPLE, ""+card.name.getId());
    }

    public static ThumbBox fromCard (MemberCard card)
    {
        return fromCard(card, MediaDescSize.THUMBNAIL_SIZE);
    }
}
