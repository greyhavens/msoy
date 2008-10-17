//
// $Id$

package client.shop;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.item.SideBar;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.util.Link;

/**
 * Displays favorites of a particular member of one or all item types.
 */
public class FavoritesPanel extends HorizontalPanel
{
    public FavoritesPanel (CatalogModels models, final int memberId, final byte itemType, int pageNo)
    {
        setStyleName("shopPanel"); // hijack the complex stylings of the shop panel
        addStyleName("favoritesPanel");
        setVerticalAlignment(HasAlignment.ALIGN_TOP);

        final String header = (itemType == Item.NOT_A_TYPE) ? _msgs.allFavorites() :
            _msgs.favoriteTitle(_dmsgs.xlate("pItemType" + itemType));

        add(new SideBar(new SideBar.Linker() {
            public boolean isSelected (byte type) {
                return type == itemType;
            }
            public Widget createLink (String name, byte type) {
                return Link.create(
                    name, Pages.SHOP, Args.compose(ShopPage.FAVORITES, memberId, type));
            }
        }, true, null));
        add(WidgetUtil.makeShim(10, 10));

        ListingGrid faves = new ListingGrid(HEADER_HEIGHT) {
            @Override protected void displayPageFromClick (int page) {
                Link.go(Pages.SHOP, Args.compose(ShopPage.FAVORITES, memberId, itemType, page));
            }
            @Override protected String getEmptyMessage () {
                return _msgs.noFavorites();
            }
            @Override protected void addCustomControls (FlexTable controls) {
                super.addCustomControls(controls);
                controls.setText(0, 0, header);
            }
            @Override protected void displayResults (int start, int count, List<ListingCard> list) {
                super.displayResults(start, count, list);
                updatePageTitle();
            }
        };
        faves.setModel(_model = models.getFavoritesModel(memberId, itemType), pageNo);
        add(faves);
        add(WidgetUtil.makeShim(10, 10));
    }

    @Override // from Widget
    protected void onLoad ()
    {
        super.onLoad();
        updatePageTitle();
    }

    protected void updatePageTitle ()
    {
        MemberName noter = _model.getNoter();
        if (noter != null) {
            CShell.frame.setTitle(_msgs.memberFavorites(noter.toString()));
        }
    }

    protected CatalogModels.Favorites _model;

    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);

    protected static final int HEADER_HEIGHT = 15 /* gap */;
}
