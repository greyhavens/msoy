//
// $Id$

package client.inventory;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.SimpleDataModel;

/**
 * Displays a set of sub-items on an item's detail page.
 */
public class SubItemPanel extends VerticalPanel
{
    public SubItemPanel (byte type, ItemPanel parent)
    {
        _type = type;
        _parent = parent;
        _contents = new PagedGrid(2, ItemPanel.COLUMNS) {
            protected Widget createWidget (Object item) {
                return new ItemContainer(_parent, (Item)item, null);
            }
            protected String getEmptyMessage () {
                return CInventory.msgs.panelNoItems(CInventory.dmsgs.getString("itemType" + _type));
            }
        };
        _contents.addStyleName("inventoryContents");
        add(_contents);
    }

    // @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (!visible || _contents.hasModel()) {
            return;
        }

        CInventory.log("Loading " + _type + "...");
        CInventory.membersvc.loadInventory(CInventory.ident, _type, new AsyncCallback() {
            public void onSuccess (Object result) {
                _contents.setModel(new SimpleDataModel((List)result), 0);
            }
            public void onFailure (Throwable caught) {
                CInventory.log("loadInventory failed", caught);
                add(new Label(CInventory.serverError(caught)));
            }
        });
    }

    protected byte _type;
    protected ItemPanel _parent;
    protected PagedGrid _contents;
}
