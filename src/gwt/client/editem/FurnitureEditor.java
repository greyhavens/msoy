//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Furniture;
import com.threerings.msoy.item.data.all.Item;

import client.util.RowPanel;

/**
 * A class for creating and editing {@link Furniture} digital items.
 */
public class FurnitureEditor extends ItemEditor
{
    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _furniture = (Furniture)item;
        _hotSpotX.setText("" + _furniture.hotSpotX);
        _hotSpotY.setText("" + _furniture.hotSpotY);
    }

    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        return new Furniture();
    }

    @Override // from ItemEditor
    protected void addFurniUploader ()
    {
        addSpacer();
        addRow(_emsgs.editorFurniTab(), createFurniUploader(true, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                if (!isValidPrimaryMedia(desc)) {
                    return _emsgs.errFurniNotFlash();
                }
                _item.furniMedia = desc;
                return null;
            }
        }), _emsgs.editorFurniTitle());

        RowPanel hsrow = new RowPanel();
        hsrow.add(_hotSpotX = new TextBox());
        hsrow.add(_hotSpotY = new TextBox());
        _hotSpotX.setVisibleLength(5);
        _hotSpotY.setVisibleLength(5);
        addRow(_emsgs.furniHotSpot(), hsrow);
        addTip(_emsgs.furniHotSpotTip());
    }

    @Override // from ItemEditor
    protected void setHash (String id, String mediaHash, int mimeType, int constraint,
                            int width, int height)
    {
        super.setHash(id, mediaHash, mimeType, constraint, width, height);
        if (Item.FURNI_MEDIA.equals(id) && MediaDesc.isImage((byte)mimeType)) {
            _hotSpotX.setText("" + (width/2));
            _hotSpotY.setText("" + height);
        }
    }

    @Override // from ItemEditor
    protected void prepareItem ()
        throws Exception
    {
        super.prepareItem();
        try {
            _furniture.hotSpotX = Short.parseShort(_hotSpotX.getText());
        } catch (Exception e) {
            // ignore-a-saurus
        }
        try {
            _furniture.hotSpotY = Short.parseShort(_hotSpotY.getText());
        } catch (Exception e) {
            // ignore-a-saurus
        }
    }

    protected Furniture _furniture;
    protected TextBox _hotSpotX;
    protected TextBox _hotSpotY;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
