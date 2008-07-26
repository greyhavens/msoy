//
// $Id$

package client.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.item.data.all.Item;

import client.images.stuff.StuffImages;
import client.shell.CShell;
import client.shell.Page;

/**
 * Displays a way to navigate our stuff. Used on the Me page and the My Stuff page which is why it
 * lives in util.
 */
public class StuffNaviBar extends SmartTable
{
    public StuffNaviBar (byte selectedType)
    {
        super("stuffNaviBar", 0, 0);

        int col = 0;
        setWidget(0, col++, WidgetUtil.makeShim(55, 10));

        for (int ii = 0; ii < Item.TYPES.length; ii++) {
            byte type = Item.TYPES[ii];
            final String tip = CShell.dmsgs.getString("pItemType" + type);
            final FocusPanel link = new FocusPanel();
            if (selectedType == type) {
                link.setWidget(SELECTED[ii].createImage());
            } else {
                final Image hover = SELECTED[ii].createImage();
                hover.addClickListener(Link.createListener(Page.STUFF, ""+type));
                final Image normal = NORMAL[ii].createImage();
                link.setWidget(normal);
                link.addMouseListener(new MouseListenerAdapter() {
                    public void onMouseEnter (Widget sender) {
                        setText(1, 0, tip);
                        link.setWidget(hover);
                    }
                    public void onMouseLeave (Widget sender) {
                        setText(1, 0, CShell.cmsgs.snbTitle());
                        link.setWidget(normal);
                    }
                });
            }
            setWidget(0, col++, link, 1, "Link");
        }

        setWidget(0, col++, WidgetUtil.makeShim(55, 10));

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
