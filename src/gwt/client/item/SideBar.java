//
// $Id$

package client.item;

import java.util.Map;

import com.google.common.collect.Maps;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import client.images.item.ItemImages;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import com.threerings.msoy.item.data.all.MsoyItemType;

/**
 * Shown next to our catalog listings and our catalog landing page.
 */
public class SideBar extends FlowPanel
{
	public interface Linker {
        public boolean isSelected (MsoyItemType itemType);
        public Widget createLink (String name, MsoyItemType itemType);
    }

    public SideBar (Linker linker, MsoyItemType[] items, Widget extras)
    {
        setStyleName("sideBar");

        FlowPanel navi = new FlowPanel();
        navi.setStyleName("NaviPanel");

		boolean first = true;
		for (MsoyItemType type : items) {
            ImageResource proto = IMAGES.get(type);
            if (!first) {
                // use a blank separator between game and level pack, etc.
                navi.add(MsoyUI.createLabel("", (proto == null) ? "BlankSep" : "Separator"));
            }
			first = false;
            Widget item = makeItem(linker, _dmsgs.xlateItemsType(type), type);
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

    protected Widget makeItem (Linker linker, String name, MsoyItemType itemType)
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

    protected static final Map<MsoyItemType, ImageResource> IMAGES = Maps.newHashMap();
    static {
        IMAGES.put(MsoyItemType.AUDIO, _itemImages.audio());
        IMAGES.put(MsoyItemType.AVATAR, _itemImages.avatar());
        IMAGES.put(MsoyItemType.DECOR, _itemImages.backdrop());
        IMAGES.put(MsoyItemType.FURNITURE, _itemImages.furniture());
        IMAGES.put(MsoyItemType.LAUNCHER, _itemImages.game());
        IMAGES.put(MsoyItemType.PET, _itemImages.pet());
        IMAGES.put(MsoyItemType.PHOTO, _itemImages.photo());
        IMAGES.put(MsoyItemType.TOY, _itemImages.toy());
        IMAGES.put(MsoyItemType.VIDEO, _itemImages.video());
    }
}
