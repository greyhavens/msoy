//
// $Id$

package com.threerings.msoy.ui {

import flash.events.Event;

public class StarsEvent extends Event
{
    public var rating :Number;

    public function StarsEvent (type :String, rating :Number)
    {
        super(type, false, false);
        this.rating = rating;
    }
}

}
