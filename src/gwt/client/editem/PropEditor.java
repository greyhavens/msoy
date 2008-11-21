//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Prop;

/**
 * A class for creating and editing {@link Prop} digital items.
 */
public class PropEditor extends SubItemEditor
{
    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _prop = (Prop)item;
    }

    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        return new Prop();
    }

    @Override // from ItemEditor
    protected String getFurniTabText ()
    {
        return _emsgs.propLabel();
    }

    @Override // from ItemEditor
    protected String getFurniTitleText ()
    {
        return _emsgs.propTip();
    }

    @Override // from ItemEditor
    protected String invalidPrimaryMediaMessage ()
    {
        return _emsgs.errPropNotFlash();
    }


    protected Prop _prop;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
