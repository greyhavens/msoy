//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.TabPanel;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Pet;
import com.threerings.msoy.item.web.MediaDesc;

/**
 * A class for creating and editing {@link Pet} digital items.
 */
public class PetEditor extends ItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _pet = (Pet)item;
    }

    // @Override from ItemEditor
    public Item createBlankItem ()
    {
        return new Pet();
    }

    // @Override from ItemEditor
    protected void createFurniUploader (TabPanel tabs)
    {
        // pets are special; their furni media are their primary media
        String title = _ctx.emsgs.petMainTitle();
        _furniUploader = createUploader(Item.FURNI_MEDIA, title, false, new MediaUpdater() {
            public String updateMedia (MediaDesc desc) {
                if (!desc.hasFlashVisual()) {
                    return _ctx.emsgs.errPetNotFlash();
                }
                _item.furniMedia = desc;
                return null;
            }
        });
        tabs.add(_furniUploader, _ctx.emsgs.petMainTab());
    }

    protected Pet _pet;
}
