//
// $Id$

package client.item;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.gwt.ItemServiceAsync;
import com.threerings.msoy.item.gwt.MemberItemInfo;

import client.ui.MsoyUI;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

public class FavoriteIndicator extends VerticalPanel
{
    public FavoriteIndicator(Item item, MemberItemInfo memberItemInfo)
    {
        super();
        _item = item;
        _memberItemInfo = memberItemInfo;

        setWidth("60px");
        setHorizontalAlignment(ALIGN_CENTER);
        setVerticalAlignment(ALIGN_TOP);

        ToggleButton favoriteToggle = new ToggleButton(ADD_FAVORITE_IMAGE, FAVORITE_IMAGE);
        favoriteToggle.setStyleName(STYLE_FAVORITE_TOGGLE);
        favoriteToggle.addStyleName("actionLabel");
        favoriteToggle.setDown(memberItemInfo.favorite);
        favoriteToggle.addClickListener(new FavoriteClickListener());
        add(favoriteToggle);

        // add label below the toggle button
        Label label = new Label(_imsgs.favorite());
        label.setStyleName(STYLE_FAVORITE_TEXT);
        add(label);
    }

    protected class FavoriteClickListener implements ClickListener
    {
        public void onClick (Widget sender) {
            final boolean favorite = ((ToggleButton) sender).isDown();
            ItemIdent item = _item.getIdent();
            _itemsvc.setFavorite(item, favorite, new MsoyCallback<Void>() {
                public void onSuccess (Void result) {
                    _memberItemInfo.favorite = favorite;
                }
            });
        }
    }

    protected Item _item;
    protected MemberItemInfo _memberItemInfo;

    protected static final ItemMessages _imsgs = GWT.create(ItemMessages.class);
    protected static final ItemServiceAsync _itemsvc = (ItemServiceAsync) ServiceUtil.bind(
        GWT.create(ItemService.class), ItemService.ENTRY_POINT);

    protected static final String STYLE_FAVORITE_TEXT = "favoriteText";
    protected static final String STYLE_FAVORITE_TOGGLE = "favoriteToggle";
    protected static final Image FAVORITE_IMAGE = MsoyUI.createImage(
        "/images/ui/favorites/favorite.png", null);
    protected static final Image ADD_FAVORITE_IMAGE = MsoyUI.createImage(
        "/images/ui/favorites/add_favorite.png", null);
}
