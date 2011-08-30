//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.whirled.game.xml.WhirledGameParser;

import com.threerings.toybox.server.persist.GameRecord;

import com.threerings.msoy.game.xml.MsoyGameParser;

/**
 * Extends the ToyBox GameRecord with MetaSOY specific bits.
 */
public class ToyBoxGameRecord extends GameRecord
{
    @Override // from GameRecord
    protected WhirledGameParser createParser () {
        return new MsoyGameParser();
    }
}
