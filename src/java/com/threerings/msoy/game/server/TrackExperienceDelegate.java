//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;
import com.threerings.msoy.data.HomePageItem;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.parlor.game.server.GameManagerDelegate;

/**
 * Tracks the user's experience playing a game.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class TrackExperienceDelegate extends GameManagerDelegate
{
    public TrackExperienceDelegate (GameContent content)
    {
        _content = content;
    }

    @Override
    public void bodyEntered (int bodyOid)
    {
        final PlayerObject plobj = (PlayerObject)_omgr.getObject(bodyOid);
        int memberId = plobj.memberName.getMemberId();
        
        _worldClient.addExperience(memberId, HomePageItem.ACTION_GAME, 
            Integer.toString(_content.game.gameId));
    }

    /** Game description. */
    protected final GameContent _content;

    @Inject protected WorldServerClient _worldClient;
}
