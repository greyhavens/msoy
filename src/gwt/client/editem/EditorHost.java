//
// $Id$

package client.editem;

import com.threerings.msoy.item.data.all.Item;

/**
 * Implemented by objects that wish to host an {@link ItemEditor}.
 */
public interface EditorHost
{
    /**
     * Called by an active {@link ItemEditor} when it is ready to go away (either the editing is
     * done or the user canceled).
     *
     * @param item if the editor was creating a new item, the new item should be passed to this
     * method so that it can be added to the display.
     */
    void editComplete (Item item);
}
