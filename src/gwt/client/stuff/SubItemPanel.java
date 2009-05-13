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
import client.util.InfoCallback;
import client.util.NaviUtil;

/**
 * Displays a set of sub-items on an item's detail page.
 */
public class SubItemPanel extends PagedGrid<Item>
{
    public SubItemPanel (InventoryModels models, int memberId, byte type, int suiteId,
                         boolean allowCreate)
    {
        super(ROWS, StuffPanel.COLUMNS, PagedGrid.NAV_ON_BOTTOM);
        addStyleName("subInventoryContents");

        _models = models;
        _memberId = memberId;
        _type = type;
        _suiteId = suiteId;

        // we can't add this in addCustomControls as that happens in our superclass constructor
        _create.addClickHandler(NaviUtil.onCreateItem(_type, suiteId));

        // if our parent is an original item, allow creation of subitems
        _create.setVisible(allowCreate);
    }

    @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (!visible || hasModel()) {
            return;
        }
        _models.loadSubModel(_memberId, _type, _suiteId, new InfoCallback<DataModel<Item>>() {
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
        _create = new Button(_msgs.panelCreateNew()); // ClickHandler added later
        controls.setWidget(0, 0, _create);
    }

    protected InventoryModels _models;
    protected int _memberId;
    protected byte _type;
    protected int _suiteId;
    protected Button _create;

    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final StuffMessages _msgs = GWT.create(StuffMessages.class);

    protected static final int ROWS = 20;
}
