//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ListBox;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Prize;

import client.shell.DynamicLookup;
import client.ui.NumberTextBox;

/**
 * A class for creating and editing {@link Prize} digital items.
 */
public class PrizeEditor extends SubItemEditor
{
    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        return new Prize();
    }

    @Override // from ItemEditor
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

    @Override // from ItemEditor
    protected void addInfo ()
    {
        super.addInfo();

        addSpacer();

        _targetType = new ListBox();
        addRow(_emsgs.prizeTargetType(), _targetType = new ListBox());
        _targetType.addItem(_emsgs.prizeSelectType(), "0");
        for (int ii = 0; ii < Item.GIFT_TYPES.length; ii++) {
            byte type = Item.GIFT_TYPES[ii];
            _targetType.addItem(_dmsgs.xlate("itemType" + type), ""+type);
        }

        // TODO: display a UI where they can select an item from their inventory that has a
        // non-zero catalog id
        _targetCatalogId = new NumberTextBox(false, 10, 10);
        addRow(_emsgs.prizeTargetId(), _targetCatalogId);
        addTip(_emsgs.prizeTargetIdTip());
    }

    @Override // from ItemEditor
    protected void addDescription ()
    {
        // we want no description
    }

    @Override // from ItemEditor
    protected void addExtras ()
    {
        // we don't have a furni media or a thumbnail media
    }

    @Override // from ItemEditor
    protected void prepareItem ()
        throws Exception
    {
        super.prepareItem();

        int selidx = _targetType.getSelectedIndex();
        _prize.targetType = Byte.parseByte(_targetType.getValue(selidx));
        if (_prize.targetType == 0) {
            throw new Exception(_emsgs.prizePleaseSelectType());
        }

        _prize.targetCatalogId = _targetCatalogId.getValue().intValue();
        if (_prize.targetCatalogId <= 0) {
            throw new Exception(_emsgs.prizePleaseSelectTarget());
        }
    }

    protected Prize _prize;

    protected ListBox _targetType;
    protected NumberTextBox _targetCatalogId;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
