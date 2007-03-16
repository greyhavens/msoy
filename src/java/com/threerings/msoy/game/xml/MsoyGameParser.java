//
// $Id$

package com.threerings.msoy.game.xml;

import org.apache.commons.digester.Digester;
import com.samskivert.xml.SetFieldRule;

import com.threerings.toybox.xml.GameParser;

import com.threerings.msoy.game.data.MsoyGameDefinition;
import com.threerings.msoy.game.data.MsoyMatchConfig;

import com.threerings.parlor.game.data.GameConfig;

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

    @Override // from GameParser
    protected void addMatchParsingRules (Digester digester, String type)
        throws Exception
    {
        if (("" + GameConfig.SEATED_GAME).equals(type)) {
            digester.push(new MsoyMatchConfig());
            digester.addRule("game/match/min_seats", new SetFieldRule("minSeats"));
            digester.addRule("game/match/max_seats", new SetFieldRule("maxSeats"));
            digester.addRule("game/match/start_seats", new SetFieldRule("startSeats"));
            digester.addRule("game/match/watchable", new SetFieldRule("watchable"));
        } else if (("" + GameConfig.PARTY).equals(type)) {
            // TODO: only display seat settings for non-party games (in GameEditor), as those 
            // settings are ignored for party games anyway
            MsoyMatchConfig config = new MsoyMatchConfig();
            config.minSeats = config.maxSeats = config.startSeats = 1;
            config.isPartyGame = true;
            config.watchable = false;
            digester.push(config);
        } else {
            throw new Exception("Unknown match type '" + type + "'.");
        }
    }
}
