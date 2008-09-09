//
// $Id$

package client.stuff;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.DataModel;

import client.shell.DynamicMessages;
import client.util.MsoyCallback;
import client.util.NaviUtil;

/**
 * Displays a set of sub-items on an item's detail page.
 */
public class SubItemPanel extends PagedGrid<Item>
{
    public SubItemPanel (ItemDataModel models, byte type, Item parent)
    {
        super(ROWS, ItemPanel.COLUMNS, PagedGrid.NAV_ON_BOTTOM);
        addStyleName("subInventoryContents");

        _models = models;
        _type = type;
        _parent = parent;

        // if our parent is an original item, allow creation of subitems
        boolean allowCreate = _parent.sourceId == 0;
        _create.setVisible(allowCreate);
        if (allowCreate) {
            _create.addClickListener(
                NaviUtil.onCreateItem(_type, _parent.getType(), _parent.itemId));
        }
    }

    @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (!visible || hasModel()) {
            return;
        }
        _models.loadModel(_type, _parent.getSuiteId(), new MsoyCallback<DataModel<Item>>() {
            public void onSuccess (DataModel<Item> model) {
                setModel(model, 0);
            }
         });
    }

    @Override // from PagedGrid
    protected Widget createWidget (Item item)
    {
        return new SubItemEntry(item);
    }

    @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return CStuff.msgs.panelNoItems(_dmsgs.getString("itemType" + _type));
    }

    @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true;
    }

    @Override // from PagedGrid
    protected void addCustomControls (FlexTable controls)
    {
        _create = new Button(CStuff.msgs.panelCreateNew());
        controls.setWidget(0, 0, _create);
    }

    protected ItemDataModel _models;
    protected byte _type;
    protected Item _parent;
    protected Button _create;

    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);

    protected static final int ROWS = 2;
}
