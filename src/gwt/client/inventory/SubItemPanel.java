//
// $Id$

package client.inventory;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.SubItem;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.SimpleDataModel;

import client.editem.EditorHost;
import client.editem.ItemEditor;

/**
 * Displays a set of sub-items on an item's detail page.
 */
public class SubItemPanel extends VerticalPanel
    implements EditorHost
{
    public SubItemPanel (byte type, Item parent, final ItemPanel panel)
    {
        _type = type;
        _parent = parent;
        _contents = new PagedGrid(ROWS, ItemPanel.COLUMNS, PagedGrid.NAV_ON_TOP) {
            protected Widget createWidget (Object item) {
                return new SubItemEntry(SubItemPanel.this, panel, (Item)item);
            }
            protected String getEmptyMessage () {
                return CInventory.msgs.panelNoItems(CInventory.dmsgs.getString("itemType" + _type));
            }
            protected boolean displayNavi (int items) {
                return items > _rows * _cols;
            }
        };
        _contents.addStyleName("subInventoryContents");
        add(_contents);

        add(WidgetUtil.makeShim(1, 10));

        _create = new Button(CInventory.msgs.panelCreateNew());
        _create.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                ItemEditor editor = ItemEditor.createItemEditor(_type, SubItemPanel.this);
                if (editor != null) {
                    _create.setEnabled(false);
                    editor.setItem(editor.createBlankItem());
                    editor.setParentItem(_parent.getIdent());
                    editor.show();
                }
            }
        });
        add(_create);
    }

    // from EditorHost
    public void editComplete (Item item)
    {
        _create.setEnabled(true);

        if (item != null) {
            // refresh our item list
            _items.remove(item);
            _items.add(0, item);
            _contents.setModel(new SimpleDataModel(_items), 0);
        }
    }

    // @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (!visible || _contents.hasModel()) {
            return;
        }

        CInventory.membersvc.loadInventory(
            CInventory.ident, _type, _parent.getSuiteId(), new AsyncCallback() {
            public void onSuccess (Object result) {
                _contents.setModel(new SimpleDataModel(_items = (List)result), 0);
            }
            public void onFailure (Throwable caught) {
                CInventory.log("loadInventory failed", caught);
                add(new Label(CInventory.serverError(caught)));
            }
        });
    }

    protected byte _type;
    protected Item _parent;

    protected PagedGrid _contents;
    protected Button _create;

    protected List _items;

    protected static final int ROWS = 5;
}
