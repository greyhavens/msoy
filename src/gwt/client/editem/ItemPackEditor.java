//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.TabPanel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemPack;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.shell.CShell;
import client.util.MsoyUI;
import client.util.RowPanel;

/**
 * A class for creating and editing {@link ItemPack} digital items.
 */
public class ItemPackEditor extends SubItemEditor
{
    // @Override from ItemEditor
    public Item createBlankItem ()
    {
        return new ItemPack();
    }

    // @Override // from ItemEditor
    protected void populateInfoTab (FlexTable info)
    {
        super.populateInfoTab(info);

        addSpacer(info);
        CheckBox box = new CheckBox();
        box.setChecked(true);
        box.setEnabled(false);
        addInfoRow(info, CShell.emsgs.packPremium(), box);
        addInfoTip(info, CShell.emsgs.ipackPremiumTip());
    }

    // @Override from ItemEditor
    protected void createFurniUploader (TabPanel tabs)
    {
        // item packs' furni media are their primary media
        String title = CShell.emsgs.ipackMainTitle();
        _furniUploader = createUploader(Item.FURNI_MEDIA, title, false, new MediaUpdater() {
            public String updateMedia (MediaDesc desc, int width, int height) {
                // TODO: validate media type
                _item.furniMedia = desc;
                return null;
            }
        });
        tabs.add(_furniUploader, CShell.emsgs.ipackMainTab());
    }
}
