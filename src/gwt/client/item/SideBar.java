//
// $Id$

package client.item;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;

import client.images.item.ItemImages;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;

/**
 * Shown next to our catalog listings and our catalog landing page.
 */
public class SideBar extends FlowPanel
{
    public interface Linker {
        public boolean isSelected (byte itemType);
        public Widget createLink (String name, byte itemType);
    }

    public SideBar (Linker linker, boolean showAll, Widget extras)
    {
        setStyleName("sideBar");

        if (showAll) {
            init(linker, Item.FAVORITE_TYPES, extras);
        } else {
            init(linker, Item.SHOP_TYPES, extras);
        }
    }

    public SideBar (Linker linker, byte[] itemTypes, Widget extras)
    {
        setStyleName("sideBar");
        init(linker, itemTypes, extras);
    }

    protected void init (Linker linker, byte[] itemTypes, Widget extras)
    {
//         addText(_msgs.sideBarCats(), 1, "Title");

        FlowPanel navi = new FlowPanel();
        navi.setStyleName("NaviPanel");

        for (int ii = 0; ii < itemTypes.length; ii++) {
            byte type = itemTypes[ii];
            ImageResource proto = IMAGES.get(type);
            if (ii > 0) {
                // use a blank separator between game and level pack, etc.
                navi.add(MsoyUI.createLabel("", (proto == null) ? "BlankSep" : "Separator"));
            }
            Widget item = makeItem(linker, _dmsgs.xlate("pItemType" + type), type);
            if (proto != null) {
                navi.add(new Image(proto));
            } else {
                navi.add(new Image(_itemImages.blank()));
                item.addStyleName("SubCell"); // items that lack an image are subordinate
            }
            navi.add(item);
        }
        add(navi);

        if (extras != null) {
            add(extras);
        }
    }

    protected Widget makeItem (Linker linker, String name, byte itemType)
    {
        Widget itemWidget;
        if (linker.isSelected(itemType)) {
            itemWidget = MsoyUI.createLabel(name, "Selected");
        } else {
            itemWidget = linker.createLink(name, itemType);
        }
        itemWidget.addStyleName("Cell");
        return itemWidget;
    }

    protected static final ItemMessages _msgs = GWT.create(ItemMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final ItemImages _itemImages = GWT.create(ItemImages.class);

    protected static final Map<Byte, ImageResource> IMAGES = new HashMap<Byte, ImageResource>();
    static {
        IMAGES.put(Item.AUDIO, _itemImages.audio());
        IMAGES.put(Item.AVATAR, _itemImages.avatar());
        IMAGES.put(Item.DECOR, _itemImages.backdrop());
        IMAGES.put(Item.FURNITURE, _itemImages.furniture());
        IMAGES.put(Item.LAUNCHER, _itemImages.game());
        IMAGES.put(Item.PET, _itemImages.pet());
        IMAGES.put(Item.PHOTO, _itemImages.photo());
        IMAGES.put(Item.TOY, _itemImages.toy());
        IMAGES.put(Item.VIDEO, _itemImages.video());
    }
}
