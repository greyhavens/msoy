//
// $Id$

package client.stuff;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;

import client.item.DoListItemPopup;
import client.util.NaviUtil;

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

        // If this item is a clone, that's all folks
        if (item.sourceId != 0) {
            return;
        }

        int row = getRowCount();
        String btitle = item.isListedOriginal() ? _msgs.detailSubUplist() : _msgs.detailList();
        _list = new Button(btitle, new ClickListener() {
            public void onClick (Widget sender) {
                DoListItemPopup.show(item, null, SubItemEntry.this);
            }
        });
        _list.addStyleName("tinyButton");
        setWidget(row, 0, _list);

        Button button = new Button(
            _msgs.detailSubEdit(), NaviUtil.onEditItem(item.getType(), item.itemId));
        button.addStyleName("tinyButton");
        setWidget(row, 1, button);
    }

    // from DoListItemPopup.ListedListener
    public void itemListed (Item item, boolean updated)
    {
        // if this was a first time listing, change "List..." to "Update listing..."
        if (!updated && item.isListedOriginal()) {
            _list.setText(_msgs.detailUplist());
        }
    }

    @Override // from ItemBox
    protected int getColumns ()
    {
        return 2;
    }

    protected Button _list;

    protected static final StuffMessages _msgs = GWT.create(StuffMessages.class);
}
