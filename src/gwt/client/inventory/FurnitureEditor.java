//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Furniture;
import com.threerings.msoy.item.web.MediaDesc;

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
    protected void createInterface (VerticalPanel contents, TabPanel tabs)
    {
        super.createInterface(contents, tabs);

        // currently nothing to do here
    }

    // @Override from ItemEditor
    protected Item createBlankItem ()
    {
        return new Furniture();
    }

    protected Furniture _furniture;
}
