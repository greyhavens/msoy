//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

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
    protected void createFurniUploader (TabPanel tabs)
    {
        // pets are special; their furni media are their primary media
        String title = "Pet as seen in the World";
        _furniUploader = new MediaUploader(Item.FURNI_MEDIA, title, false, new MediaUpdater() {
            public String updateMedia (MediaDesc desc) {
                if (!desc.hasFlashVisual()) {
                    return "Pets must be a web-viewable image type.";
                }
                _item.furniMedia = desc;
                return null;
            }
        });
        tabs.add(_furniUploader, "Pet Media");
    }

    // @Override from ItemEditor
    protected Item createBlankItem ()
    {
        return new Pet();
    }

    protected Pet _pet;
}
