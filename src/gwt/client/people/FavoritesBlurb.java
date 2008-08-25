//
// $Id$

package client.people;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.profile.gwt.ProfileService;

import client.item.ListingBox;
import client.shell.Args;
import client.shell.Pages;

/**
 * A blurb containing a few of the member's most recent favorites.
 */
public class FavoritesBlurb extends Blurb
{
    @Override// from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);
        setHeader(CPeople.msgs.favoritesTitle());

        SmartTable grid = new SmartTable();
        for (int ii = 0; ii < pdata.faves.size(); ii++) {
            grid.setWidget(0, ii, new ListingBox(pdata.faves.get(ii)));
        }
        setContent(grid);

        setFooterLink(CPeople.msgs.seeMoreFavorites(), Pages.SHOP,
            Args.compose("f", pdata.name.getMemberId()));
    }
}
