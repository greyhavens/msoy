//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.item.data.all.Furniture;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.util.RowPanel;

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
        _hotSpotX.setText("" + _furniture.hotSpotX);
        _hotSpotY.setText("" + _furniture.hotSpotY);
    }

    // @Override from ItemEditor
    public Item createBlankItem ()
    {
        return new Furniture();
    }

    // @Override from ItemEditor
    protected void populateInfoTab (FlexTable info)
    {

        super.populateInfoTab(info);
    }

    // @Override from ItemEditor
    protected void createFurniUploader (TabPanel tabs)
    {
        FlexTable furni = new FlexTable();

        String title = CEditem.emsgs.editorFurniTitle();
        _furniUploader = createUploader(Item.FURNI_MEDIA, title, false, new MediaUpdater() {
            public String updateMedia (MediaDesc desc, int width, int height) {
                if (!desc.hasFlashVisual()) {
                    return CEditem.emsgs.errFurniNotFlash();
                }
                _item.furniMedia = desc;
                return null;
            }
        });
        addInfoRow(furni, _furniUploader);

        RowPanel hsrow = new RowPanel();
        hsrow.add(_hotSpotX = new TextBox());
        hsrow.add(_hotSpotY = new TextBox());
        _hotSpotX.setVisibleLength(5);
        _hotSpotY.setVisibleLength(5);
        addInfoRow(furni, CEditem.emsgs.furniHotSpot(), hsrow);
        addInfoTip(furni, CEditem.emsgs.furniHotSpotTip());

        tabs.add(furni, CEditem.emsgs.editorFurniTab());
    }

    // @Override from ItemEditor
    protected void setHash (
        String id, String mediaHash, int mimeType, int constraint, int width, int height,
        String thumbMediaHash, int thumbMimeType, int thumbConstraint)
    {
        super.setHash(id, mediaHash, mimeType, constraint, width, height,
                      thumbMediaHash, thumbMimeType, thumbConstraint);
        if (Item.FURNI_MEDIA.equals(id) && MediaDesc.isImage((byte)mimeType)) {
            _hotSpotX.setText("" + (width/2));
            _hotSpotY.setText("" + height);
        }
    }

    // @Override // from ItemEditor
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
}
