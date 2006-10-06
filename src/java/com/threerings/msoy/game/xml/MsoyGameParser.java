//
// $Id$

package com.threerings.msoy.game.xml;

import com.threerings.toybox.xml.GameParser;

import com.threerings.msoy.game.data.MsoyGameDefinition;

/**
 * Parses game definitions into instances of {@link MsoyGameDefinition}.
 */
public class MsoyGameParser extends GameParser
{
    @Override // from GameParser
    protected String getGameDefinitionClass ()
    {
        return MsoyGameDefinition.class.getName();
    }
}
