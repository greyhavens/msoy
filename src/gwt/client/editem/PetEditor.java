//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Pet;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * A class for creating and editing {@link Pet} digital items.
 */
public class PetEditor extends ItemEditor
{
    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _pet = (Pet)item;
    }

    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        return new Pet();
    }

    @Override // from ItemEditor
    protected void addFurniUploader ()
    {
        // pets are special; their furni media are their primary media
        addSpacer();
        addRow(_emsgs.petLabel(), createFurniUploader(false, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (!isValidPrimaryMedia(desc)) {
                    return _emsgs.errPetNotFlash();
                }
                _item.furniMedia = desc;
                return null;
            }
        }), _emsgs.petTip());
    }

    protected Pet _pet;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
