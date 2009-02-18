//
// $Id$

package client.item;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.gwt.ItemServiceAsync;
import com.threerings.msoy.item.gwt.MemberItemInfo;

import client.images.misc.MiscImages;
import client.ui.MsoyUI;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

public class FavoriteIndicator extends FlowPanel
{
    public FavoriteIndicator (Item item, MemberItemInfo memberItemInfo)
    {
        setStyleName("favorite");
        _item = item;
        _memberItemInfo = memberItemInfo;

        add(new Label(_imsgs.favorite()));

        ToggleButton toggle = new ToggleButton(_mimgs.add_favorite().createImage(),
                                               _mimgs.favorite().createImage());
        toggle.addStyleName("actionLabel");
        toggle.setDown(memberItemInfo.favorite);
        toggle.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                final boolean favorite = ((ToggleButton) sender).isDown();
                _itemsvc.setFavorite(_item.getType(), _item.catalogId, favorite,
                    new MsoyCallback<Void>() {
                    public void onSuccess (Void result) {
                        _memberItemInfo.favorite = favorite;
                    }
                });
            }
        });
        add(toggle);
    }

    protected Item _item;
    protected MemberItemInfo _memberItemInfo;

    protected static final ItemMessages _imsgs = GWT.create(ItemMessages.class);
    protected static final ItemServiceAsync _itemsvc = (ItemServiceAsync) ServiceUtil.bind(
        GWT.create(ItemService.class), ItemService.ENTRY_POINT);
    protected static final MiscImages _mimgs = GWT.create(MiscImages.class);
}
