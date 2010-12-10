//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ListBox;

import com.samskivert.util.ByteEnumUtil;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.data.all.Prize;

import client.shell.DynamicLookup;
import client.ui.NumberTextBox;

/**
 * A class for creating and editing {@link Prize} digital items.
 */
public class PrizeEditor extends IdentGameItemEditor
{
    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        Prize prize = new Prize();
        prize.targetType = MsoyItemType.AVATAR;
        return prize;
    }

    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _prize = (Prize)item;

        String targetType = Byte.toString(_prize.targetType.toByte());
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
        for (MsoyItemType type : MsoyItemType.values()) {
            if (type.isGiftType()) {
                _targetType.addItem(_dmsgs.xlateItemType(type), Byte.toString(type.toByte()));
            }
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
        byte byteVal = Byte.parseByte(_targetType.getValue(selidx));
        if (byteVal == 0) {
            throw new Exception(_emsgs.prizePleaseSelectType());
        }
        _prize.targetType = ByteEnumUtil.fromByte(MsoyItemType.class, byteVal);

        _prize.targetCatalogId = _targetCatalogId.getNumber().intValue();
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
