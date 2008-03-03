//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.HorizontalPanel;

import com.threerings.msoy.item.data.all.Item;

import client.shell.Application;
import client.shell.CShell;
import client.shell.Page;

/**
 * Displays a way to navigate our stuff. Used on the Me page and the My Stuff page which is why it
 * lives in util.
 */
public class StuffNaviBar extends HorizontalPanel
{
    public StuffNaviBar (byte selectedType) // TODO
    {
        setStyleName("stuffNaviBar");
        for (int ii = 0; ii < Item.TYPES.length; ii++) {
            byte type = Item.TYPES[ii];
            String path = Item.getDefaultThumbnailMediaFor(type).getMediaPath();
            String tip = CShell.dmsgs.getString("pItemType" + type);
            add(Application.createImageLink(path, tip, Page.STUFF, ""+type));
        }
    }
}
