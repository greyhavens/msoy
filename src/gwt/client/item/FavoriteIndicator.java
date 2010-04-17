//
// $Id$

package client.item;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ToggleButton;


import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.gwt.ItemServiceAsync;
import com.threerings.msoy.item.gwt.MemberItemInfo;

import client.images.misc.MiscImages;
import client.util.InfoCallback;

public class FavoriteIndicator extends FlowPanel
{
    public FavoriteIndicator (Item item, MemberItemInfo memberItemInfo)
    {
        setStyleName("favorite");
        _item = item;
        _memberItemInfo = memberItemInfo;

        add(new Label(_imsgs.favorite()));

        ToggleButton toggle = new ToggleButton(
            new Image(_mimgs.add_favorite()), new Image(_mimgs.favorite()));
        toggle.addStyleName("actionLabel");
        toggle.setDown(memberItemInfo.favorite);
        toggle.addClickHandler(new ClickHandler() {
            public void onClick (ClickEvent event) {
                final boolean favorite = ((ToggleButton) event.getSource()).isDown();
                _itemsvc.setFavorite(_item.getType(), _item.catalogId, favorite,
                    new InfoCallback<Void>() {
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
    protected static final ItemServiceAsync _itemsvc = GWT.create(ItemService.class);
    protected static final MiscImages _mimgs = GWT.create(MiscImages.class);
}
