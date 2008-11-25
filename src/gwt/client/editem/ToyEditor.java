//
// $Id$

package client.editem;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Toy;

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
    protected String getFurniType ()
    {
        return TYPE_FLASH_ONLY_REMIXABLE;
    }

    @Override // from ItemEditor
    protected boolean generateFurniThumbnail ()
    {
        return false;
    }

    protected Toy _toy;
}
