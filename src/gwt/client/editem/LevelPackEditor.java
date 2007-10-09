//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.TabPanel;

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
        addInfoRow(info, CEditem.emsgs.packPremium(), _premium = new CheckBox());
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
    protected CheckBox _premium;
}
