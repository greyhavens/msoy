//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.util.ActionScript;

import com.threerings.crowd.data.BodyObject;

import com.threerings.parlor.data.Table;
import com.threerings.parlor.data.TableConfig;
import com.threerings.parlor.game.data.GameConfig;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * An msoy-specific table.
 */
public class MsoyTable extends Table
{
    /** Head shots for each occupant. */
    public MediaDesc[] headShots;

    /** Suitable for unserialization. */
    public MsoyTable ()
    {
    }

    @Override @ActionScript(omit=true)
    public void init (int lobbyOid, TableConfig tconfig, GameConfig config)
    {
        super.init(lobbyOid, tconfig, config);

        headShots = new MediaDesc[occupants.length];
    }

    @Override @ActionScript(omit=true)
    public void setOccupantPos (int position, BodyObject body)
    {
        super.setOccupantPos(position, body);

        PlayerObject member = (PlayerObject) body;
        headShots[position] = member.getHeadShotMedia();
    }

    @Override @ActionScript(omit=true)
    public void clearOccupantPos (int position)
    {
        super.clearOccupantPos(position);

        headShots[position] = null;
    }
}
