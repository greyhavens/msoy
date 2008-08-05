//
// $Id$

package client.item;

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
import client.shell.DynamicMessages;
import client.shell.Pages;
import client.shell.ShellMessages;
import client.util.Link;

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
            final String tip = _dmsgs.getString("pItemType" + type);
            final FocusPanel link = new FocusPanel();
            if (selectedType == type) {
                link.setWidget(SELECTED[ii].createImage());
            } else {
                final Image hover = SELECTED[ii].createImage();
                hover.addClickListener(Link.createListener(Pages.STUFF, ""+type));
                final Image normal = NORMAL[ii].createImage();
                link.setWidget(normal);
                link.addMouseListener(new MouseListenerAdapter() {
                    public void onMouseEnter (Widget sender) {
                        setText(1, 0, tip);
                        link.setWidget(hover);
                    }
                    public void onMouseLeave (Widget sender) {
                        setText(1, 0, _cmsgs.snbTitle());
                        link.setWidget(normal);
                    }
                });
            }
            setWidget(0, col++, link, 1, "Link");
        }

        setWidget(0, col++, WidgetUtil.makeShim(55, 10));

        setText(1, 0, _cmsgs.snbTitle(), col, "Label");
    }

    protected static final StuffImages _simgs = (StuffImages)GWT.create(StuffImages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);

    /** Our normal item images, in the same order as {@link Item#TYPES}. */
    protected static final AbstractImagePrototype[] NORMAL = {
        _simgs.avatar(), _simgs.furniture(), _simgs.decor(), _simgs.toy(),
        _simgs.pet(), _simgs.game(), _simgs.photo(), _simgs.audio(), _simgs.video()
    };

    /** Our selected item images, in the same order as {@link Item#TYPES}. */
    protected static final AbstractImagePrototype[] SELECTED = {
        _simgs.avatar_s(), _simgs.furniture_s(), _simgs.decor_s(), _simgs.toy_s(),
        _simgs.pet_s(), _simgs.game_s(), _simgs.photo_s(), _simgs.audio_s(), _simgs.video_s()
    };
}
