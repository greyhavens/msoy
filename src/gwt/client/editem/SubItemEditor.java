//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;

/**
 * Handles the editing of sub-items.
 */
public abstract class SubItemEditor extends ItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _subi = (SubItem)item;
        safeSetText(_ident, _subi.ident);
    }

    // @Override // from ItemEditor
    protected void populateInfoTab (FlexTable info)
    {
        super.populateInfoTab(info);

        addSpacer(info);
        addInfoRow(info, CEditem.emsgs.subIdent(), bind(_ident = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _subi.ident = text;
            }
        }));
        _ident.setMaxLength(SubItem.MAX_IDENT_LENGTH);
        addInfoTip(info, CEditem.emsgs.subIdentTip());
    }

    protected SubItem _subi;
    protected TextBox _ident;
}
