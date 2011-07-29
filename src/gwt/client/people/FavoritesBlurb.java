//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.web.gwt.Pages;

import client.item.ListingBox;

/**
 * A blurb containing a few of the member's most recent favorites.
 */
public class FavoritesBlurb extends Blurb
{
    @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return !pdata.faves.isEmpty();
    }

    @Override// from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);
        setHeader(_msgs.favoritesTitle());

        SmartTable grid = new SmartTable();
        for (int ii = 0; ii < pdata.faves.size(); ii++) {
            grid.setWidget(0, ii, ListingBox.newBox(pdata.faves.get(ii)));
        }
        setContent(grid);

        setFooterLink(_msgs.seeMoreFavorites(), Pages.SHOP, "f", pdata.name.getId());
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
}
