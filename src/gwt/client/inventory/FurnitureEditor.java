//
// $Id$

package client.inventory;

import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Furniture;
import com.threerings.msoy.item.web.MediaDesc;

/**
 * A class for creating and editing {@link Furniture} digital items.
 */
public class FurnitureEditor extends ItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _furniture = (Furniture)item;
    }

    // @Override from ItemEditor
    protected void createMainInterface (VerticalPanel main)
    {
        // furni is special; the furni media is its primary media
        String title = "Furniture Image";
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
        return new Furniture();
    }

    protected Furniture _furniture;
}
