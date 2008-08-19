package client.stuff;

import client.shell.Args;
import client.shell.Pages;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.SimplePanel;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemListInfo;
import com.threerings.msoy.stuff.gwt.StuffService;
import com.threerings.msoy.stuff.gwt.StuffServiceAsync;

/**
 * A panel used to display a grid of favorite items for a member.
 *
 * @author mjensen
 */
public class FavoritesPanel extends SimplePanel
{
    /**
     * The default number of rows to display.
     */
    public static final int DEFAULT_ROWS = 1;

    /**
     * The default number of columns to display in the favorites grid.
     */
    public static final int DEFAULT_COLUMNS = 5;

    public FavoritesPanel (Pages parentPage)
    {
        this(parentPage, DEFAULT_ROWS, DEFAULT_COLUMNS);
    }

    public FavoritesPanel (Pages parentPage, int rows, int cols)
    {
        this(parentPage, rows, cols, true);
    }

    public FavoritesPanel (Pages parentPage, int rows, int cols, boolean displayNavigation)
    {
        _parentPage = parentPage;
        _rows = rows;
        _cols = cols;
        _displayNavigation = displayNavigation;
    }

    /**
     * Delegates args from the parent page when onHistoryChanged() is called.
     */
    public void update (int memberId, String[] prefixArgs, Args allPageArgs)
    {
        // save the args in case the _favoritesPanel is not initialized yet
        _prefixArgs = prefixArgs;
        _lastArgs = allPageArgs;

        if (memberId != _memberId && !MemberName.isGuest(memberId)) {
            // if the member Id has changed, then we have some reloading to do
            setMemberId(memberId);

        } else if (_favoritesGridPanel != null) {
            // otherwise just update the current contents
            _favoritesGridPanel.setPrefixArgs(_prefixArgs);
            _favoritesGridPanel.onHistoryChanged(allPageArgs);
        }
    }

    protected void setMemberId (int memberId)
    {
        // update the prefix args to contain the correct memberId
        _prefixArgs[_prefixArgs.length-1] = String.valueOf(memberId);

        _memberId = memberId;
        _stuffsvc.getFavoriteListInfo(_memberId, new MsoyCallback<ItemListInfo>() {
            public void onSuccess (ItemListInfo favoriteListInfo) {
                setListId(favoriteListInfo.listId);
            }
        });
    }

    /**
     * Filters the types of items to be displayed.
     */
    public void setItemType (byte itemType)
    {
        if (itemType != _itemType) {
            _itemType = itemType;
            // TODO store the item type in a single location. For example, have the favorites grid
            // pull the current type from the model.
            if (_favoriteModel != null) {
                _favoriteModel.setItemType(itemType);
            }
            if (_favorites != null) {
                _favorites.setItemType(itemType);
            }
        }
    }

    protected void setListId (int listId)
    {
        // create the favorites model
        _favoriteModel = new ItemListDataModel(_itemType);
        _favoriteModel.setListId(listId);
        // order favorites starting with the most recently favorited items
        _favoriteModel.setDescending(true);

        _favorites = new ItemGrid(_parentPage, _itemType, _rows, _cols, CStuff.msgs
            .favorites(), CStuff.msgs.noFavorites());
        _favorites.setDisplayNavigation(_displayNavigation);
        _favorites.setModel(_favoriteModel, 0);

        _favoritesGridPanel = new ItemGridPanel(_parentPage, _favorites, _favoriteModel);
        _favoritesGridPanel.setPrefixArgs(_prefixArgs);
        if (_lastArgs != null) {
            _favoritesGridPanel.onHistoryChanged(_lastArgs);
        }
        setWidget(_favoritesGridPanel);
    }

    protected ItemListDataModel _favoriteModel;

    protected ItemGrid _favorites;

    protected ItemGridPanel _favoritesGridPanel;

    /**
     * The most recent args passed to onHistoryChanged(). This is used in the case that the
     * _favoritesPanel was not yet initialized when onHistoryChanged() is first called.
     */
    protected Args _lastArgs;

    /**
     * The parent page on which this panel lives.
     */
    protected Pages _parentPage;

    protected int _rows;

    protected int _cols;

    /**
     * Whether or not to display the grid paging controls.
     */
    protected boolean _displayNavigation;

    protected String[] _prefixArgs = ItemGridPanel.NO_PREFIX_ARGS;;

    protected byte _itemType = Item.NOT_A_TYPE;

    /**
     * The member whose favorites will be displayed.
     */
    protected int _memberId;

    protected static final StuffServiceAsync _stuffsvc = (StuffServiceAsync) ServiceUtil.bind(GWT
        .create(StuffService.class), StuffService.ENTRY_POINT);
}
