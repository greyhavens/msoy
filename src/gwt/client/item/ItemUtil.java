//
// $Id$

package client.item;

import com.threerings.msoy.item.web.Item;

/**
 * Contains utility methods for item related user interface business.
 */
public class ItemUtil
{
    /**
     * Returns the name of this item or a properly translated string indicating that it has no
     * name.
     */
    public static String getName (ItemContext ctx, Item item)
    {
        return getName(ctx, item, false);
    }

    /**
     * Returns the truncated name of this item or a properly translated string indicating that it
     * has no name.
     */
    public static String getName (ItemContext ctx, Item item, boolean truncate)
    {
        String name = (item.name.trim().length() == 0) ? "<no name>" : item.name;
        if (name.length() > 32 && truncate) {
            name = name.substring(0, 29) + "...";
        }
        return name;
    }

    /**
     * Returns the description of this item or a properly translated string indicating that it has
     * no description.
     */
    public static String getDescription (ItemContext ctx, Item item)
    {
        return (item.description.trim().length() == 0) ?
            "No description provided for this item." : item.description;
    }
}
