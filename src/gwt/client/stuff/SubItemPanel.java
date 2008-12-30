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

import client.shell.DynamicLookup;
import client.util.MsoyCallback;
import client.util.NaviUtil;

/**
 * Displays a set of sub-items on an item's detail page.
 */
public class SubItemPanel extends PagedGrid<Item>
{
    public SubItemPanel (InventoryModels models, byte type, Item parent)
    {
        super(ROWS, StuffPanel.COLUMNS, PagedGrid.NAV_ON_BOTTOM);
        addStyleName("subInventoryContents");

        _models = models;
        _type = type;
        _parent = parent;

        // we can't add this in addCustomControls as that happens in our superclass constructor
        _create.addClickListener(NaviUtil.onCreateItem(_type, _parent.getType(), _parent.itemId));

        // if our parent is an original item, allow creation of subitems
        _create.setVisible(_parent.sourceId == 0);
    }

    @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (!visible || hasModel()) {
            return;
        }
        _models.loadSubModel(_type, _parent.getSuiteId(), new MsoyCallback<DataModel<Item>>() {
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
        return _msgs.panelNoItems(_dmsgs.xlate("itemType" + _type));
    }

    @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true;
    }

    @Override // from PagedGrid
    protected void addCustomControls (FlexTable controls)
    {
        _create = new Button(_msgs.panelCreateNew()); // ClickListener added later
        controls.setWidget(0, 0, _create);
    }

    protected InventoryModels _models;
    protected byte _type;
    protected Item _parent;
    protected Button _create;

    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final StuffMessages _msgs = GWT.create(StuffMessages.class);

    protected static final int ROWS = 20;
}
