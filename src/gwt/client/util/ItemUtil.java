//
// $Id$

package client.util;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;

import client.shell.Application;
import client.shell.CShell;

/**
 * Contains utility methods for item related user interface business.
 */
public class ItemUtil
{
    /**
     * Returns the name of this item or a properly translated string indicating that it has no
     * name.
     */
    public static String getName (Item item)
    {
        return getName(item, false);
    }

    /**
     * Returns the truncated name of this item or a properly translated string indicating that it
     * has no name.
     */
    public static String getName (Item item, boolean truncate)
    {
        String name = (item.name.trim().length() == 0) ? CShell.cmsgs.noName() : item.name;
        if (name.length() > 32 && truncate) {
            name = CShell.cmsgs.truncName(name.substring(0, 29));
        }
        return name;
    }

    /**
     * Returns the description of this item or a properly translated string indicating that it has
     * no description.
     */
    public static String getDescription (Item item)
    {
        return (item.description.trim().length() == 0) ?
            CShell.cmsgs.noDescrip() : item.description;
    }

    /**
     * Adds item specific buttons to be shown in the item detail in a member's inventory or in the
     * catalog.
     */
    public static void addItemSpecificButtons (Item item, HorizontalPanel panel)
    {
        if (item instanceof Game) {
            final String token = Application.createLinkToken("game", "" + ((Game)item).gameId);
            panel.add(new Button(CShell.cmsgs.detailPlay(), new ClickListener() {
                public void onClick (Widget sender) {
                    History.newItem(token);
                }
            }));
        }
    }
}
