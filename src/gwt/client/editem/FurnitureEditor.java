//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.data.all.MediaMimeTypes;
import com.threerings.msoy.item.data.all.Furniture;
import com.threerings.msoy.item.data.all.Item;

import client.ui.RowPanel;

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
        super.addFurniUploader();

        RowPanel hsrow = new RowPanel();
        hsrow.add(_hotSpotX = new TextBox());
        hsrow.add(_hotSpotY = new TextBox());
        _hotSpotX.setVisibleLength(5);
        _hotSpotY.setVisibleLength(5);
        addRow(_emsgs.furniHotSpot(), hsrow);
        addTip(_emsgs.furniHotSpotTip());
    }

    @Override // from ItemEditor
    protected void setHash (
        String id, String filename, String mediaHash, int mimeType, int constraint,
        /*int expiration, String signature,*/ int width, int height)
    {
        super.setHash(id, filename, mediaHash, mimeType, constraint,
            /*expiration, signature,*/ width, height);

        if (Item.FURNI_MEDIA.equals(id)) {
            maybeSetNameFromFilename(filename);
            if ( MediaMimeTypes.isImage((byte)mimeType)) {
                _hotSpotX.setText("" + (width/2));
                _hotSpotY.setText("" + height);
            }
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
