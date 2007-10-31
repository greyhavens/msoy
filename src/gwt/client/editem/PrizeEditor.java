//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Prize;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.shell.CShell;
import client.util.MsoyUI;
import client.util.NumberTextBox;
import client.util.RowPanel;

/**
 * A class for creating and editing {@link Prize} digital items.
 */
public class PrizeEditor extends SubItemEditor
{
    // @Override from ItemEditor
    public Item createBlankItem ()
    {
        return new Prize();
    }

    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _prize = (Prize)item;

        String targetType = ""+_prize.targetType;
        for (int ii = 0; ii < _targetType.getItemCount(); ii++) {
            if (_targetType.getValue(ii).equals(targetType)) {
                _targetType.setSelectedIndex(ii);
                break;
            }
        }
        _targetCatalogId.setText("" + _prize.targetCatalogId);
    }

    // @Override // from ItemEditor
    protected void createInterface (VerticalPanel contents, TabPanel tabs)
    {
        // we don't have a furni media or a thumbnail media
    }

    // @Override // from ItemEditor
    protected void populateInfoTab (FlexTable info)
    {
        super.populateInfoTab(info);

        addSpacer(info);

        _targetType = new ListBox();
        addInfoRow(info, CShell.emsgs.prizeTargetType(), _targetType = new ListBox());
        _targetType.addItem(CShell.emsgs.prizeSelectType(), "0");
        for (int ii = 0; ii < Item.GIFT_TYPES.length; ii++) {
            byte type = Item.GIFT_TYPES[ii];
            _targetType.addItem(CShell.dmsgs.getString("itemType" + type), ""+type);
        }

        // TODO: display a UI where they can select an item from their inventory that has a
        // non-zero catalog id
        _targetCatalogId = new NumberTextBox(false, 10, 10);
        addInfoRow(info, CShell.emsgs.prizeTargetId(), _targetCatalogId);
        addInfoTip(info, CShell.emsgs.prizeTargetIdTip());
    }

    // @Override // from ItemEditor
    protected void addDescription (FlexTable info)
    {
        // we want no description
    }

    // @Override // from ItemEditor
    protected void prepareItem ()
        throws Exception
    {
        super.prepareItem();

        int selidx = _targetType.getSelectedIndex();
        _prize.targetType = Byte.parseByte(_targetType.getValue(selidx));
        if (_prize.targetType == 0) {
            throw new Exception(CShell.emsgs.prizePleaseSelectType());
        }

        _prize.targetCatalogId = _targetCatalogId.getValue().intValue();
        if (_prize.targetCatalogId <= 0) {
            throw new Exception(CShell.emsgs.prizePleaseSelectTarget());
        }
    }

    protected Prize _prize;

    protected ListBox _targetType;
    protected NumberTextBox _targetCatalogId;
}
