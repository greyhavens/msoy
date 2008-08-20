//
// $Id$

package client.people;

import client.shell.Args;
import client.shell.Pages;
import client.stuff.FavoritesPanel;

import com.threerings.msoy.profile.gwt.ProfileService;

/**
 * @author mjensen
 */
public class FavoritesBlurb extends Blurb
{
    @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);
        setHeader(CPeople.msgs.favoritesTitle());
        FavoritesPanel favoritesPanel = new FavoritesPanel(Pages.PEOPLE, 1, 5, false);
        setContent(favoritesPanel);
        favoritesPanel.update(_name.getMemberId(), new String[0], new Args());
        setFooterLink(CPeople.msgs.seeMoreFavorites(pdata.name.toString()),
            Pages.SHOP, Args.compose("f", pdata.name.getMemberId()));
    }
}
