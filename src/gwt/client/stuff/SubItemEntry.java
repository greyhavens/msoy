//
// $Id$

package client.stuff;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;

/**
 * Displays information on a sub-item.
 */
public class SubItemEntry extends ItemEntry
    implements DoListItemPopup.ListedListener
{
    public SubItemEntry (final Item item)
    {
        super(item);

        SubItem sitem = (SubItem)item;
        addText(sitem.ident, getColumns(), "Ident");

        int row = getRowCount();
        String btitle = (item.catalogId == 0) ?
            CStuff.msgs.detailList() : CStuff.msgs.detailSubUplist();
        _list = new Button(btitle, new ClickListener() {
            public void onClick (Widget sender) {
                DoListItemPopup.show(item, null, SubItemEntry.this);
            }
        });
        _list.addStyleName("tinyButton");
        setWidget(row, 0, _list);

        Button button = new Button(CStuff.msgs.detailSubEdit(), new ClickListener() {
            public void onClick (Widget sender) {
                CStuff.editItem(item.getType(), item.itemId);
            }
        });
        button.addStyleName("tinyButton");
        setWidget(row, 1, button);
    }

    // from DoListItemPopup.ListedListener
    public void itemListed (Item item, boolean updated)
    {
        // if this was a first time listing, change "List..." to "Update listing..."
        if (!updated && item.catalogId != 0) {
            _list.setText(CStuff.msgs.detailUplist());
        }
    }

    // @Override // from ItemBox
    protected int getColumns ()
    {
        return 2;
    }

    protected Button _list;
}
