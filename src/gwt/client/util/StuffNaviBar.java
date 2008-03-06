//
// $Id$

package client.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.item.data.all.Item;

import client.images.stuff.StuffImages;
import client.shell.Application;
import client.shell.CShell;
import client.shell.Page;

/**
 * Displays a way to navigate our stuff. Used on the Me page and the My Stuff page which is why it
 * lives in util.
 */
public class StuffNaviBar extends SmartTable
{
    public StuffNaviBar (byte selectedType) // TODO
    {
        super("stuffNaviBar", 0, 0);

        int col = 0;
        setHTML(0, col, "&nbsp;");
        getFlexCellFormatter().setStyleName(0, col++, "Edge");
        for (int ii = 0; ii < Item.TYPES.length; ii++) {
            byte type = Item.TYPES[ii];
            String tip = CShell.dmsgs.getString("pItemType" + type);
            Widget link;
            if (selectedType == type) {
                link = SELECTED[ii].createImage();
            } else {
                link = Application.createImageLink(NORMAL[ii], tip, Page.STUFF, ""+type);
            }
            setWidget(0, col, link, 1, "Link");
            getFlexCellFormatter().setHorizontalAlignment(0, col++, HasAlignment.ALIGN_CENTER);
        }
        setHTML(0, col, "&nbsp;");
        getFlexCellFormatter().setStyleName(0, col++, "Edge");

        setText(1, 0, CShell.cmsgs.snbTitle(), col, "Label");
    }

    /** Our navigation menu images. */
    protected static StuffImages _images = (StuffImages)GWT.create(StuffImages.class);

    /** Our normal item images, in the same order as {@link Item#TYPES}. */
    protected static final AbstractImagePrototype[] NORMAL = {
        _images.avatar(), _images.furniture(), _images.decor(), _images.toy(),
        _images.pet(), _images.game(), _images.photo(), _images.audio(), _images.video()
    };

    /** Our selected item images, in the same order as {@link Item#TYPES}. */
    protected static final AbstractImagePrototype[] SELECTED = {
        _images.avatar_s(), _images.furniture_s(), _images.decor_s(), _images.toy_s(),
        _images.pet_s(), _images.game_s(), _images.photo_s(), _images.audio_s(), _images.video_s()
    };
}
