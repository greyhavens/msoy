//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.crowd.data.BodyObject;

import com.threerings.whirled.spot.data.ClusteredBodyObject;

/**
 * The base class for msoy bodies.
 */
public class MsoyBodyObject extends BodyObject
    implements ClusteredBodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>sceneId</code> field. */
    public static const SCENE_ID :String = "sceneId";

    /** The field name of the <code>clusterOid</code> field. */
    public static const CLUSTER_OID :String = "clusterOid";
    // AUTO-GENERATED: FIELDS END

    /** The sceneId we occupy. */
    public var sceneId :int;

    /** The cluster object we're in. */
    public var clusterOid :int;

    // from ScenedBodyObject
    public function getSceneId () :int
    {
        return sceneId;
    }

    // from ClusteredBodyObject
    public function getClusterOid () :int
    {
        return clusterOid;
    }

    // from ClusteredBodyObject
    public function getClusterField () :String
    {
        return CLUSTER_OID;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        sceneId = ins.readInt();
        clusterOid = ins.readInt();
    }
}
}
