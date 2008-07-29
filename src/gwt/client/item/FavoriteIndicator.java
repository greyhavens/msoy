package client.item;

import client.item.rating.ItemRatingModel;
import client.shell.CShell;
import client.shell.ShellMessages;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.gwt.ItemServiceAsync;

public class FavoriteIndicator extends CheckBox
{
    public FavoriteIndicator(ItemRatingModel model)
    {
        super(_imsgs.favorite());
        _model = model;
        
        setChecked(model.isFavorite());                
        addClickListener(new FavoriteClickListener());        
    }
    
    protected class FavoriteClickListener implements ClickListener 
    {

        public void onClick (Widget sender)
        {
            final boolean favorite = ((FavoriteIndicator) sender).isChecked();
            
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
}
