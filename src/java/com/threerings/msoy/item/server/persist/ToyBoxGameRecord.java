//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.threerings.toybox.server.persist.GameRecord;
import com.threerings.toybox.xml.GameParser;

import com.threerings.msoy.game.xml.MsoyGameParser;

/**
 * Extends the ToyBox GameRecord with MetaSOY specific bits.
 */
public class ToyBoxGameRecord extends GameRecord
{
    @Override // from GameRecord
    protected GameParser createParser () {
        return new MsoyGameParser();
    }
}
