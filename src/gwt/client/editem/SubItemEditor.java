//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;

import client.shell.CShell;

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
    protected void addInfo ()
    {
        super.addInfo();

        addSpacer();
        addRow(CShell.emsgs.subIdent(), _ident = new TextBox());
        _ident.setMaxLength(SubItem.MAX_IDENT_LENGTH);
        addTip(CShell.emsgs.subIdentTip());
    }

    // @Override // from ItemEditor
    protected void prepareItem ()
        throws Exception
    {
        super.prepareItem();

        _subi.ident = _ident.getText();
        if (!nonBlank(_subi.ident, SubItem.MAX_IDENT_LENGTH)) {
            throw new Exception(CShell.emsgs.subIdentMissing(""+SubItem.MAX_IDENT_LENGTH));
        }
    }

    protected SubItem _subi;
    protected TextBox _ident;
}
