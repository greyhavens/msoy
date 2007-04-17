//
// $Id$

package client.editem;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Furniture;

/**
 * A class for creating and editing {@link Furniture} digital items.
 */
public class FurnitureEditor extends ItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _furniture = (Furniture)item;
    }

    // @Override from ItemEditor
    public Item createBlankItem ()
    {
        return new Furniture();
    }

    protected Furniture _furniture;
}
