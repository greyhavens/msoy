//
// $Id$

package client.shop;

import client.item.FavoritesGrid;
import client.item.ItemGrid;
import client.item.ItemListDataModel;
import client.shell.CShell;
import client.shell.Pages;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.SimplePanel;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MemberItemListInfo;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.gwt.ItemServiceAsync;

/**
 * A panel used to display a grid of favorite items for a member.
 *
 * @author mjensen
 */
public class FavoritesPanel extends SimplePanel
{
    /**
     * An empty array used in the case that the are no prefix args required.
     */
    public static final String[] NO_PREFIX_ARGS = new String[0];

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

        addStyleName("favoritesPanel");
    }

    /**
     * Updates the favorites panel.
     *
     * @param prefixArgs page args that the FavoritesGrid should prepend to its links.
     */
    public void update (int memberId, byte itemType, int gridPage, String[] prefixArgs)
    {
        // save these values in case the _favoritesPanel is not initialized yet
        _itemType = itemType;
        _page = gridPage;
        _prefixArgs = prefixArgs;

        if (memberId != _memberId && !MemberName.isGuest(memberId)) {
            // if the member Id has changed, then we have some reloading to do
            setMemberId(memberId);
        } else if (_favorites != null) {
            // otherwise just update and display the grid
            displayGrid();
        }
    }

    /**
     * Filters the types of items to be displayed.
     */
    public void setItemType (byte itemType)
    {
        _itemType = itemType;
        if (_favoriteModel != null) {
            _favoriteModel.setItemType(itemType);
        }
    }

    /**
     * Returns the title that should be displayed for this panel.
     */
    @Override
    public String getTitle ()
    {
        return _title;
    }

    protected void setMemberId (int memberId)
    {
        _memberId = memberId;
        _itemsvc.getFavoriteListInfo(_memberId, new MsoyCallback<MemberItemListInfo>() {
            public void onSuccess (MemberItemListInfo favoriteListInfo) {
                _memberName = favoriteListInfo.memberName;
                setListId(favoriteListInfo.listId);
            }
        });
    }

    protected void setListId (int listId)
    {
        // create the favorites model
        _favoriteModel = new ItemListDataModel(_itemType);
        _favoriteModel.setListId(listId);
        // order favorites starting with the most recently favorited items
        _favoriteModel.setDescending(true);

        _favorites = new FavoritesGrid(_parentPage, _rows, _cols);
        _favorites.setDisplayNavigation(_displayNavigation);
        _favorites.setModel(_favoriteModel, 0);

        // if update has already been called, then display the grid
        if (_prefixArgs != null) {
            displayGrid();
        }
    }

    protected void displayGrid ()
    {
        // use a title containing the member's name
        _title = (_memberId == CShell.getMemberId()) ?
            _shopmsgs.myFavoritesTitle() : _shopmsgs.memberFavorites(_memberName);

        _favoriteModel.setItemType(_itemType);
        _favorites.setPrefixArgs(_prefixArgs);
        _favorites.setModel(_favoriteModel, _page);
        setWidget(_favorites);
    }

    protected ItemListDataModel _favoriteModel;

    protected ItemGrid _favorites;

    protected String[] _prefixArgs = NO_PREFIX_ARGS;

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

    /**
     * The current page to display in the favorite item grid.
     */
    protected int _page;

    protected byte _itemType = Item.NOT_A_TYPE;

    /**
     * The member whose favorites will be displayed.
     */
    protected int _memberId;

    /**
     * The member's name for display in the page title.
     */
    protected String _memberName;

    /**
     * The title for this panel including the current member's name.
     */
    protected String _title = _shopmsgs.myFavoritesTitle();

    protected static final ShopMessages _shopmsgs = GWT.create(ShopMessages.class);

    protected static final ItemServiceAsync _itemsvc = (ItemServiceAsync)
        ServiceUtil.bind(GWT.create(ItemService.class), ItemService.ENTRY_POINT);
}
