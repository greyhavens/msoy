//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Pet;

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
    protected String getFurniTabText ()
    {
        return _emsgs.petLabel();
    }

    @Override // from ItemEditor
    protected boolean generateFurniThumbnail ()
    {
        return false;
    }

    @Override // from ItemEditor
    protected String getFurniTitleText ()
    {
        return _emsgs.petTip();
    }

    @Override // from ItemEditor
    protected String invalidPrimaryMediaMessage ()
    {
        return _emsgs.errPetNotFlash();
    }

    @Override // from ItemEditor
    protected String getFurniType ()
    {
        return TYPE_FLASH_ONLY;
    }

    protected Pet _pet;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
