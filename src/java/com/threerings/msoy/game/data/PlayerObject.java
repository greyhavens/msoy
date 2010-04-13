//
// $Id$

package com.threerings.msoy.game.data;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Generated;

import com.threerings.util.Name;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.data.TokenRing;

import com.whirled.game.data.PropertySpaceMarshaller;
import com.whirled.game.data.PropertySpaceObject;
import com.whirled.game.data.WhirledPlayerObject;
import com.whirled.game.server.PropertySpaceHelper;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.MsoyUserObject;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberMailUtil;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.data.all.VizMemberName;

import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.avrg.data.AVRGameOccupantInfo;

import com.threerings.msoy.party.data.PartySummary;

import static com.threerings.msoy.Log.log;

/**
 * Contains information on a player logged on to an MSOY Game server.
 */
public class PlayerObject extends WhirledPlayerObject
    implements MsoyUserObject, PropertySpaceObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>memberName</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String MEMBER_NAME = "memberName";

    /** The field name of the <code>tokens</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String TOKENS = "tokens";

    /** The field name of the <code>game</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String GAME = "game";

    /** The field name of the <code>coins</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String COINS = "coins";

    /** The field name of the <code>bars</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String BARS = "bars";

    /** The field name of the <code>humanity</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String HUMANITY = "humanity";

    /** The field name of the <code>visitorInfo</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String VISITOR_INFO = "visitorInfo";

    /** The field name of the <code>propertyService</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String PROPERTY_SERVICE = "propertyService";
    // AUTO-GENERATED: FIELDS END

    /** The name and id information for this user. */
    public VizMemberName memberName;

    /** The tokens defining the access controls for this user. */
    public MsoyTokenRing tokens;

    /** The game summary for the game that the player is lobbying for or currently playing. */
    public GameSummary game;

    /** How many coins we've got jangling around on our person. */
    public int coins;

    /** The number of bars the member has currently in their account. */
    public int bars;

    /** Our current assessment of how likely to be human this member is, in [0, {@link
     * MsoyCodes#MAX_HUMANITY}]. */
    public int humanity;

    /** Player's referral information. */
    public VisitorInfo visitorInfo;

    /** Service for setting player properties. */
    public PropertySpaceMarshaller propertyService;

    /**
     * Get the media to use as our headshot.
     */
    public MediaDesc getHeadShotMedia ()
    {
        return memberName.getPhoto();
    }

    /**
     * Returns true if this user is a permaguest.
     */
    public boolean isPermaguest ()
    {
        return MemberMailUtil.isPermaguest(username.toString());
    }

    /**
     * Returns {@link #visitorInfo}.id but logs a warning and stack trace if visitorInfo is null.
     */
    public String getVisitorId ()
    {
        if (visitorInfo == null) {
            log.warning("Member missing visitorInfo", "who", who(), new Exception());
            return "";
        }
        return visitorInfo.id;
    }

    // from interface MsoyUserObject
    public MemberName getMemberName ()
    {
        return memberName;
    }

    // from interface MsoyUserObject
    public int getMemberId ()
    {
        return memberName.getMemberId();
    }

    // from interface MsoyUserObject
    public float getHumanity ()
    {
        return humanity / (float)MsoyCodes.MAX_HUMANITY;
    }

    // from interface MsoyUserObject
    public void setParty (PartySummary summary)
    {
        _party = summary;
        // we do not separately track a partyId here.
    }

    // from interface MsoyUserObject
    public PartySummary getParty ()
    {
        return _party;
    }

    @Override // from BodyObject
    public TokenRing getTokens ()
    {
        return tokens;
    }

    @Override // from BodyObject
    public Name getVisibleName ()
    {
        return memberName;
    }

    // from PropertySpaceObject
    public Map<String, Object> getUserProps ()
    {
        return _props;
    }

    // from PropertySpaceObject
    public Set<String> getDirtyProps ()
    {
        return _dirty;
    }

    @Override // from BodyObject
    public OccupantInfo createOccupantInfo (PlaceObject plObj)
    {
        if (plObj instanceof ParlorGameObject) {
            return new ParlorGameOccupantInfo(this);

        } else if (plObj instanceof AVRGameObject)  {
            return new AVRGameOccupantInfo(this);

        } else {
            return super.createOccupantInfo(plObj);
        }
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>memberName</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setMemberName (VizMemberName value)
    {
        VizMemberName ovalue = this.memberName;
        requestAttributeChange(
            MEMBER_NAME, value, ovalue);
        this.memberName = value;
    }

    /**
     * Requests that the <code>tokens</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setTokens (MsoyTokenRing value)
    {
        MsoyTokenRing ovalue = this.tokens;
        requestAttributeChange(
            TOKENS, value, ovalue);
        this.tokens = value;
    }

    /**
     * Requests that the <code>game</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setGame (GameSummary value)
    {
        GameSummary ovalue = this.game;
        requestAttributeChange(
            GAME, value, ovalue);
        this.game = value;
    }

    /**
     * Requests that the <code>coins</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setCoins (int value)
    {
        int ovalue = this.coins;
        requestAttributeChange(
            COINS, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.coins = value;
    }

    /**
     * Requests that the <code>bars</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setBars (int value)
    {
        int ovalue = this.bars;
        requestAttributeChange(
            BARS, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.bars = value;
    }

    /**
     * Requests that the <code>humanity</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setHumanity (int value)
    {
        int ovalue = this.humanity;
        requestAttributeChange(
            HUMANITY, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.humanity = value;
    }

    /**
     * Requests that the <code>visitorInfo</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setVisitorInfo (VisitorInfo value)
    {
        VisitorInfo ovalue = this.visitorInfo;
        requestAttributeChange(
            VISITOR_INFO, value, ovalue);
        this.visitorInfo = value;
    }

    /**
     * Requests that the <code>propertyService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setPropertyService (PropertySpaceMarshaller value)
    {
        PropertySpaceMarshaller ovalue = this.propertyService;
        requestAttributeChange(
            PROPERTY_SERVICE, value, ovalue);
        this.propertyService = value;
    }
    // AUTO-GENERATED: METHODS END

    @Override // from BodyObject
    protected void addWhoData (StringBuilder buf)
    {
        buf.append("pid=").append(getMemberId()).append(" oid=");
        super.addWhoData(buf);
    }

    /**
     * A custom serialization method.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();

        PropertySpaceHelper.writeProperties(this, out);
    }

    /**
     * A custom serialization method.
     */
    public void readObject (ObjectInputStream ins)
        throws IOException, ClassNotFoundException
    {
        ins.defaultReadObject();

        PropertySpaceHelper.readProperties(this, ins);
    }

    /** The party to which this player belongs. */
    protected transient PartySummary _party;

    /**
     * The current state of game data.
     * On the server, this will be a byte[] for normal properties and a byte[][] for array
     * properties. On the client, the actual values are kept whole.
     */
    protected transient HashMap<String, Object> _props = new HashMap<String, Object>();

    /**
     * The persistent properties that have been written to since startup.
     */
    protected transient Set<String> _dirty = new HashSet<String>();
}
