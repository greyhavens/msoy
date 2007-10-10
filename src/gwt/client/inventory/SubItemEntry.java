//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;

import client.editem.ItemEditor;

/**
 * Displays information on a sub-item.
 */
public class SubItemEntry extends ItemEntry
{
    public SubItemEntry (SubItemPanel spanel, ItemPanel panel, Item item)
    {
        super(panel, item, null);
        _spanel = spanel;
    }

    // @Override // from ItemEntry
    public void setItem (Item item)
    {
        super.setItem(item);

        SubItem sitem = (SubItem)item;
        getFlexCellFormatter().setRowSpan(0, 0, 3);
        setText(1, 0, sitem.ident);
        getFlexCellFormatter().setStyleName(1, 0, "Ident");

        Button button = new Button(CInventory.msgs.detailEdit(), new ClickListener() {
            public void onClick (Widget sender) {
                ItemEditor editor = ItemEditor.createItemEditor(_item.getType(), _spanel);
                editor.setItem(_item);
                editor.show();
            }
        });
        button.setStyleName("tinyButton");
        setWidget(2, 0, button);
    }

    protected SubItemPanel _spanel;
}
