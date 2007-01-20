package client.item;

import client.inventory.ItemEditor;

import com.threerings.msoy.item.web.Item;

public interface ItemEditorHost
{
    /**
     * Displays a status message to the user, may be called by item editors.
     */
    void setStatus (String string);

    /**
     * Called by an active {@link ItemEditor} when it is ready to go away (either the editing is
     * done or the user canceled).
     *
     * @param item if the editor was creating a new item, the new item should be passed to this
     * method so that it can be added to the display.
     */
    void editComplete (Item item);
}
