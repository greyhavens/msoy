//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.web.client.Args;
import com.threerings.msoy.web.client.Pages;

import client.trophy.TrophyGrid;

/**
 * Displays a member's recently earned trophies.
 */
public class TrophiesBlurb extends Blurb
{
    @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return (pdata.trophies != null && pdata.trophies.size() > 0);
    }

    @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);
        setHeader(_msgs.trophiesTitle());

        // display our trophies in a nice grid
        SmartTable grid = new SmartTable(0, 4);
        Trophy[] tvec = pdata.trophies.toArray(new Trophy[pdata.trophies.size()]);
        TrophyGrid.populateTrophyGrid(grid, tvec);
        setContent(grid);

        setFooterLink(_msgs.seeAll(), Pages.GAMES,
                      Args.compose("t", pdata.name.getMemberId()));
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
}
