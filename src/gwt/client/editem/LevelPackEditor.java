//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.LevelPack;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.util.MsoyUI;
import client.util.RowPanel;

/**
 * A class for creating and editing {@link LevelPack} digital items.
 */
public class LevelPackEditor extends ItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _pack = (LevelPack)item;
        safeSetText(_ident, _pack.ident);
        _premium.setChecked(_pack.premium);
    }

    // @Override from ItemEditor
    public Item createBlankItem ()
    {
        return new LevelPack();
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

        addInfoRow(info, CEditem.emsgs.lpackPremium(), _premium = new CheckBox());
        addInfoTip(info, CEditem.emsgs.lpackPremiumTip());

        super.populateInfoTab(info);
    }

    // @Override from ItemEditor
    protected void createFurniUploader (TabPanel tabs)
    {
        // level packs' furni media are their primary media
        String title = CEditem.emsgs.lpackMainTitle();
        _furniUploader = createUploader(Item.FURNI_MEDIA, title, false, new MediaUpdater() {
            public String updateMedia (MediaDesc desc, int width, int height) {
                // TODO: validate media type
                _item.furniMedia = desc;
                return null;
            }
        });
        tabs.add(_furniUploader, CEditem.emsgs.lpackMainTab());
    }

    // @Override // from ItemEditor
    protected void prepareItem ()
        throws Exception
    {
        super.prepareItem();
        _pack.premium = _premium.isChecked();
    }

    protected LevelPack _pack;
    protected TextBox _ident;
    protected CheckBox _premium;
}
