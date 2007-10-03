//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemPack;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.util.MsoyUI;
import client.util.RowPanel;

/**
 * A class for creating and editing {@link ItemPack} digital items.
 */
public class ItemPackEditor extends ItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _pack = (ItemPack)item;
        safeSetText(_ident, _pack.ident);
    }

    // @Override from ItemEditor
    public Item createBlankItem ()
    {
        return new ItemPack();
    }

    // @Override // from ItemEditor
    protected void populateInfoTab (FlexTable info)
    {
        addInfoRow(info, CEditem.emsgs.packIdent(), bind(_ident = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _pack.ident = text;
            }
        }));
        _ident.setMaxLength(Game.MAX_IDENT_LENGTH);
        addInfoTip(info, CEditem.emsgs.packIdentTip());

        CheckBox box = new CheckBox();
        box.setChecked(true);
        box.setEnabled(false);
        addInfoRow(info, CEditem.emsgs.packPremium(), box);
        addInfoTip(info, CEditem.emsgs.ipackPremiumTip());

        super.populateInfoTab(info);
    }

    // @Override from ItemEditor
    protected void createFurniUploader (TabPanel tabs)
    {
        // item packs' furni media are their primary media
        String title = CEditem.emsgs.ipackMainTitle();
        _furniUploader = createUploader(Item.FURNI_MEDIA, title, false, new MediaUpdater() {
            public String updateMedia (MediaDesc desc, int width, int height) {
                // TODO: validate media type
                _item.furniMedia = desc;
                return null;
            }
        });
        tabs.add(_furniUploader, CEditem.emsgs.ipackMainTab());
    }

    protected ItemPack _pack;
    protected TextBox _ident;
}
