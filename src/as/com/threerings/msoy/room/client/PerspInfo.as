//
// $Id$

package com.threerings.msoy.room.client {

import flash.geom.Point;

/**
 * Holds perspectivization info calculated by the RoomView.
 */
public class PerspInfo
{
    /** The projected coordinate of the source's pixel (0, 0). */
    public var p0 :Point;

    /** The height of the 0 edge. */
    public var height0 :Number;

    /** The projected coordinate of the source's pixel (N, M), where the
     * N = source width - 1, M = source height - 1. */
    public var pN :Point;

    /** The height at the N edge. */
    public var heightN :Number;

    /** The hotspot, perspectivized. */
    public var hotSpot :Point;

    public function PerspInfo (
        p0 :Point, height0 :Number, pN :Point, heightN :Number, hotSpot :Point)
    {
        this.p0 = p0;
        this.height0 = height0;
        this.pN = pN;
        this.heightN = heightN;
        this.hotSpot = hotSpot;
    }
}
}
