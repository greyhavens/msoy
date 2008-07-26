//
// $Id$

package client.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Page;
import client.shell.ShellMessages;

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
        String name = (item.name == null || item.name.trim().length() == 0) ?
            _cmsgs.noName() : item.name;
        if (name.length() > 32 && truncate) {
            name = _cmsgs.truncName(name.substring(0, 29));
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
            _cmsgs.noDescrip() : item.description;
    }

    /**
     * Adds item specific buttons to be shown in the item detail in a member's inventory or in the
     * catalog.
     */
    public static void addItemSpecificButtons (final Item item, HorizontalPanel panel)
    {
        if (item instanceof Game) {
            panel.add(new Button(_cmsgs.detailPlay(), new ClickListener() {
                public void onClick (Widget sender) {
                    Link.go(Page.GAMES, Args.compose("d" , ((Game)item).gameId));
                }
            }));
        }
    }

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
