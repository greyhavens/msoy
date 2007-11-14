//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.TabPanel;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Mob;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.shell.CShell;

/**
 * A class for creating and editing {@link Mob} digital items.
 */
public class MobEditor extends SubItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _mob = (Mob)item;
    }

    // @Override from ItemEditor
    public Item createBlankItem ()
    {
        return new Mob();
    }

    // @Override from ItemEditor
    protected void createFurniUploader (TabPanel tabs)
    {
        // mobs are special; their furni media are their primary media
        String title = CShell.emsgs.mobMainTitle();
        _furniUploader = createUploader(Item.FURNI_MEDIA, title, false, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (!desc.hasFlashVisual()) {
                    return CShell.emsgs.errMobNotFlash();
                }
                _item.furniMedia = desc;
                return null;
            }
        });
        tabs.add(_furniUploader, CShell.emsgs.mobMainTab());
    }

    protected Mob _mob;
}
