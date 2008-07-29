package client.item;

import client.item.rating.ItemRatingModel;
import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.gwt.ItemServiceAsync;

public class FavoriteIndicator extends VerticalPanel
{
    public FavoriteIndicator(ItemRatingModel model)
    {
        super();
        _model = model;
        setHorizontalAlignment(ALIGN_CENTER);
        setVerticalAlignment(ALIGN_TOP);

        ToggleButton favoriteToggle = new ToggleButton(ADD_FAVORITE_IMAGE, FAVORITE_IMAGE);
        favoriteToggle.setDown(model.isFavorite());
        favoriteToggle.addClickListener(new FavoriteClickListener());
        add(favoriteToggle);

        // add label below the toggle button
        Label label = new Label(_imsgs.favorite());
        add(label);
    }

    protected class FavoriteClickListener implements ClickListener
    {
        public void onClick (Widget sender)
        {
            final boolean favorite = ((ToggleButton) sender).isDown();

            ItemIdent item = _model.getItem().getIdent();
            _itemsvc.setFavorite(CShell.ident, item, favorite, new MsoyCallback<Void>() {
                public void onSuccess (Void result) {
                    _model.setFavorite(favorite);
                }
            });
        }
    }

    protected ItemRatingModel _model;

    protected static final ItemMessages _imsgs = GWT.create(ItemMessages.class);

    protected static final ItemServiceAsync _itemsvc =
        (ItemServiceAsync) ServiceUtil.bind(GWT.create(ItemService.class), ItemService.ENTRY_POINT);

    protected static final String STYLE_FAVORITE = "favoriteText";

    protected static final Image FAVORITE_IMAGE = MsoyUI.createImage("/images/ui/favorites/favorite.png", null);

    protected static final Image ADD_FAVORITE_IMAGE = MsoyUI.createImage("/images/ui/favorites/add_favorite.png", null);
}
