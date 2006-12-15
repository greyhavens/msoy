//
// $Id$

package client.inventory;

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
    protected void createMainInterface (VerticalPanel main)
    {
        super.createMainInterface(main);

        // pets are special; their furni media are their primary media
        String title = "Main Pet media";
        main.add(_furniUploader = new MediaUploader(Item.FURNI_ID, title, true, new MediaUpdater() {
            public String updateMedia (MediaDesc desc) {
                if (!desc.hasFlashVisual()) {
                    return "Furniture must be a web-viewable image type.";
                }
                _item.furniMedia = desc;
                recenter(true);
                return null;
            }
        }));
    }

    // @Override from ItemEditor
    protected Item createBlankItem ()
    {
        return new Pet();
    }

    protected Pet _pet;
}
