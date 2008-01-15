//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.TabPanel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.LevelPack;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.shell.CShell;
import client.util.MsoyUI;
import client.util.RowPanel;

/**
 * A class for creating and editing {@link LevelPack} digital items.
 */
public class LevelPackEditor extends SubItemEditor
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
    protected void addInfo ()
    {
        super.addInfo();

        addSpacer();
        addRow(CShell.emsgs.packPremium(), _premium = new CheckBox());
        addTip(CShell.emsgs.lpackPremiumTip());
    }

    // @Override from ItemEditor
    protected void addFurniUploader ()
    {
        // level packs' furni media are their primary media
        addSpacer();
        addRow(CShell.emsgs.lpackMainTab(), createFurniUploader(false, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                // TODO: validate media type
                _item.furniMedia = desc;
                return null;
            }
        }), CShell.emsgs.lpackMainTitle());
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
