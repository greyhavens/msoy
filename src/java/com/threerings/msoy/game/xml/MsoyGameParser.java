//
// $Id$

package com.threerings.msoy.game.xml;

import java.io.IOException;
import java.io.StringReader;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import com.samskivert.xml.SetFieldRule;

import com.threerings.ezgame.data.GameDefinition;
import com.threerings.ezgame.data.TableMatchConfig;
import com.threerings.ezgame.xml.GameParser;

import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.game.data.MsoyGameDefinition;
import com.threerings.msoy.game.data.MsoyMatchConfig;

import com.threerings.parlor.game.data.GameConfig;

/**
 * Parses game definitions into instances of {@link MsoyGameDefinition}.
 */
public class MsoyGameParser extends GameParser
{
    public MsoyGameParser ()
    {
        _digester.addRule("game/lwjgl", new Rule() {
            public void begin (String namespace, String name, Attributes attrs)
                throws Exception {
                ((MsoyGameDefinition)digester.peek()).lwjgl = true;
            }
        });
    }

    /**
     * Parses a game definition from the supplied {@link Game} object.
     *
     * @exception IOException thrown if an error occurs reading the file.
     * @exception SAXException thrown if an error occurs parsing the XML.
     */
    public GameDefinition parseGame (Game game)
        throws IOException, SAXException
    {
        MsoyGameDefinition gameDef = (MsoyGameDefinition)parseGame(new StringReader(game.config));
        gameDef.setMediaPath(game.gameMedia.getMediaPath());
        return gameDef;
    }

    @Override // from GameParser
    protected String getGameDefinitionClass ()
    {
        return MsoyGameDefinition.class.getName();
    }

    @Override // from GameParser
    protected void addMatchParsingRules (final Digester digester, String type)
        throws Exception
    {
        int mtype = Integer.valueOf(type);
        if (mtype == GameConfig.SEATED_GAME) {
            digester.addRule("game/match/unwatchable", new Rule() {
                public void begin (String namespace, String name, Attributes attrs)
                    throws Exception {
                    ((MsoyMatchConfig)digester.peek()).unwatchable = true;
                }
            });
        }
        super.addMatchParsingRules(digester, type);
        ((MsoyMatchConfig)digester.peek()).type = mtype;
    }

    @Override // from GameParser
    protected TableMatchConfig createMatchConfig ()
    {
        return new MsoyMatchConfig();
    }
}
