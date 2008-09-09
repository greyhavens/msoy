//
// $Id$

package client.stuff;

import com.google.gwt.user.client.ui.FlowPanel;
import com.threerings.msoy.item.data.all.Item;

import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.Link;

/**
 * Displays a way to navigate our stuff. Used on the Me page and the My Stuff page which is why it
 * lives in util.
 */
public class StuffNaviBar extends FlowPanel
{
    public StuffNaviBar (byte selectedType)
    {
        setStyleName("stuffNaviBar");

        for (byte type : Item.TYPES) {
            add(MsoyUI.createActionImage("/images/stuff/navbar_" + type + ".png",
                Link.createListener(Pages.STUFF, "" + type)));
        }
        add(MsoyUI.createSimplePanel("clear", null));
    }
}

