//
// $Id$
//
// Copyright (c) 2007 Three Rings Design, Inc.  Please do not redistribute.

package com.whirled {

import flash.display.DisplayObject;

/**
 * Defines actions, accessors and callbacks available to all Pets.
 */
public class PetControl extends ActorControl
{
    /**
     * Creates a controller for a Pet. The display object is the Pet's visualization.
     */
    public function PetControl (disp :DisplayObject)
    {
        super(disp);
    }

    // from WhirledControl
    override protected function populateProperties (o :Object) :void
    {
        super.populateProperties(o);

        // TODO
    }

    override protected function isAbstract () :Boolean
    {
        return false;
    }
}
}
