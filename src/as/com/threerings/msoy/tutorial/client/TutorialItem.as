//
// $Id$

package com.threerings.msoy.tutorial.client {

import flash.errors.IllegalOperationError;

/**
 * Holds the values used by the tutorial director to display and handle tutorial items.
 */
internal class TutorialItem
{
    /** @private */
    public function TutorialItem (kind :Kind, id :String, text :String)
    {
        this.kind = kind;
        this.id = id;
        this.text = text;
    }

    internal var kind :Kind;
    internal var id :String;
    internal var text :String;
    internal var checkAvailable :Function;
    internal var buttonText :String;
    internal var onClick :Function;
}
}
