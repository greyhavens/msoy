//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Prop;

import client.shell.CShell;

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
    protected void addFurniUploader ()
    {
        // props are special; their furni media are their primary media
        addSpacer();
        addRow(_emsgs.propLabel(), createFurniUploader(true, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (!desc.hasFlashVisual()) {
                    return _emsgs.errPropNotFlash();
                }
                _item.furniMedia = desc;
                return null;
            }
        }), _emsgs.propTip());
    }

    protected Prop _prop;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
