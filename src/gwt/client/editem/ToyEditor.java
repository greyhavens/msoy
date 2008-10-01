//
// $Id$

package client.editem;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Toy;

import client.editem.ItemEditor.MediaUpdater;

/**
 * A class for creating and editing {@link Toy} digital items.
 */
public class ToyEditor extends ItemEditor
{
    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _toy = (Toy)item;
    }

    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        return new Toy();
    }

    @Override // from ItemEditor
    protected String invalidPrimaryMediaMessage ()
    {
        return _emsgs.errToyNotFlash();
    }

    @Override // from ItemEditor
    protected void addFurniUploader ()
    {
        // do not generate a thumbnail.
        addSpacer();
        addRow(getFurniTabText(), createFurniUploader(false, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (!isValidPrimaryMedia(desc)) {
                    return invalidPrimaryMediaMessage();
                }
                _item.furniMedia = desc;
                return null;
            }
        }), getFurniTitleText());
    }

    protected Toy _toy;
}
