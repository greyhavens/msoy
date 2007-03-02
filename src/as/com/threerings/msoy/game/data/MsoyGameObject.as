package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.ezgame.data.EZGameObject;

public class MsoyGameObject extends EZGameObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>flowPerMinute</code> field. */
    public static const FLOW_PER_MINUTE :String= "flowPerMinute";
    // AUTO-GENERATED: FIELDS END

    /** The base per-minute flow rate of this game. */
    public var flowPerMinute :int;

    /** The whirled game services. */
    public var msoyGameService :MsoyGameMarshaller;

    override protected function readDefaultFields (ins :ObjectInputStream) :void
    {
        super.readDefaultFields(ins);

        flowPerMinute = ins.readInt();
        msoyGameService = (ins.readObject() as MsoyGameMarshaller);
    }
}
}
