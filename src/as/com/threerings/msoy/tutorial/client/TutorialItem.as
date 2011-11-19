//
// $Id$

package com.threerings.msoy.tutorial.client {

import com.threerings.util.Hashable;
import com.threerings.util.StringUtil;

/**
 * Holds the values used by the tutorial director to display and handle tutorial items.
 */
internal class TutorialItem
    implements Hashable
{
    /** @private */
    public function TutorialItem (kind :Kind, id :String, text :String)
    {
        this.kind = kind;
        this.id = id;
        this.text = text;
    }

    // from Hashable
    public function hashCode () :int
    {
        return 31 * kind.hashCode() + StringUtil.hashCode(id);
    }

    // from Equalable via Hashable
    public function equals (other :Object) :Boolean
    {
        return (other is TutorialItem) && (id == TutorialItem(other).id);
    }

    /**
     * Checks the availability of this tutorial item for display.
     */
    public function isAvailable () :Boolean
    {
        return checkAvailable == null || checkAvailable();
    }

    internal var kind :Kind;
    internal var id :String;
    internal var text :String;
    internal var checkAvailable :Function;
    internal var buttonText :String;
    internal var buttonCloses :Boolean;
    internal var hideClose :Boolean;
    internal var onClick :Function;
    internal var onShow :Function;
    internal var ignorable :Boolean = true;
    internal var popupHelper :PopupHelper;
    internal var finishText :String;
}
}
