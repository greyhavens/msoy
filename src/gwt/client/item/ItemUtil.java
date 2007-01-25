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
        String name = (item.name.trim().length() == 0) ? CItem.imsgs.noName() : item.name;
        if (name.length() > 32 && truncate) {
            name = CItem.imsgs.truncName(name.substring(0, 29));
        }
        return name;
    }

    /**
     * Returns the description of this item or a properly translated string indicating that it has
     * no description.
     */
    public static String getDescription (Item item)
    {
        return (item.description.trim().length() == 0) ? CItem.imsgs.noDescrip() : item.description;
    }
}
