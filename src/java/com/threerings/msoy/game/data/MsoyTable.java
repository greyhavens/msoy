//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.crowd.data.BodyObject;

import com.threerings.parlor.data.Table;
import com.threerings.parlor.data.TableConfig;
import com.threerings.parlor.game.data.GameConfig;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.item.web.MediaDesc;

/**
 * A msoy-specific table.
 */
public class MsoyTable extends Table
{
    /** Head shots for each occupant. */
    public MediaDesc[] headShots;

    /** Suitable for unserialization. */
    public MsoyTable ()
    {
    }

    @Override
    public void init (int lobbyOid, TableConfig tconfig, GameConfig config)
    {
        super.init(lobbyOid, tconfig, config);

        int size = (occupants == null) ? 0 : occupants.length;
        headShots = new MediaDesc[size];
    }

    @Override
    public void setOccupantPos (int position, BodyObject body)
    {
        super.setOccupantPos(position, body);

        MemberObject member = (MemberObject) body;
        headShots[position] = member.getHeadShotMedia();
    }

    @Override
    public void clearOccupantPos (int position)
    {
        super.clearOccupantPos(position);

        headShots[position] = null;
    }
}
