//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.IdentGameItem;

/**
 * Handles the editing of game items with an identifier.
 */
public abstract class IdentGameItemEditor extends GameItemEditor
{
    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _igitem = (IdentGameItem)item;
        safeSetText(_ident, _igitem.ident);
    }

    @Override // from ItemEditor
    protected void addInfo ()
    {
        super.addInfo();

        addSpacer();
        addRow(_emsgs.subIdent(), _ident = new TextBox());
        _ident.setMaxLength(IdentGameItem.MAX_IDENT_LENGTH);
        addTip(_emsgs.subIdentTip());
    }

    @Override // from ItemEditor
    protected void prepareItem ()
        throws Exception
    {
        super.prepareItem();

        _igitem.ident = _ident.getText();
        if (!nonBlank(_igitem.ident, IdentGameItem.MAX_IDENT_LENGTH)) {
            throw new Exception(_emsgs.subIdentMissing(""+IdentGameItem.MAX_IDENT_LENGTH));
        }
    }

    protected IdentGameItem _igitem;
    protected TextBox _ident;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
