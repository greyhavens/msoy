//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.TypedArray;
import com.threerings.io.ObjectInputStream;

import com.threerings.crowd.data.BodyObject;

import com.threerings.parlor.data.Table;
import com.threerings.parlor.data.TableConfig;
import com.threerings.parlor.game.data.GameConfig;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * A msoy-specific table.
 */
public class MsoyTable extends Table
{
    /** Head shots for each occupant. */
    public var headShots :TypedArray;

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        headShots = (ins.readObject() as TypedArray);
    }
}
}
