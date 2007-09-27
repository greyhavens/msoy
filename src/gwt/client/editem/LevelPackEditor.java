//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.TabPanel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.LevelPack;
import com.threerings.msoy.item.data.all.MediaDesc;

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
    }

    // @Override from ItemEditor
    public Item createBlankItem ()
    {
        return new LevelPack();
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

    protected LevelPack _pack;
}
