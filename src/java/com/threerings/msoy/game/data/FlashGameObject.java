//
// $Id$

package com.threerings.msoy.game.data;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import com.threerings.util.Name;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.parlor.game.data.GameObject;
import com.threerings.parlor.turn.data.TurnGameObject;

/**
 * Contains the data for a flash game.
 */
public class FlashGameObject extends GameObject
    implements TurnGameObject
{
    /** The identifier for a MessageEvent containing a user message. */
    public static final String USER_MESSAGE = "Umsg";

    /** The identifier for a MessageEvent containing game-system chat. */
    public static final String GAME_CHAT = "Uchat";

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>turnHolder</code> field. */
    public static final String TURN_HOLDER = "turnHolder";

    /** The field name of the <code>flashGameService</code> field. */
    public static final String FLASH_GAME_SERVICE = "flashGameService";
    // AUTO-GENERATED: FIELDS END

    /** The current turn holder. */
    public Name turnHolder;

    /** The service interface for requesting special things from the server. */
    public FlashGameMarshaller flashGameService;

    // from TurnGameObject
    public String getTurnHolderFieldName ()
    {
        return TURN_HOLDER;
    }

    // from TurnGameObject
    public Name getTurnHolder ()
    {
        return turnHolder;
    }

    // from TurnGameObject
    public Name[] getPlayers ()
    {
        return players;
    }

    /**
     * Called by PropertySetEvent to effect the property update.
     */
    protected void applyPropertySet (String propName, Object data, int index)
    {
        if (index != -1) {
            byte[][] arr = (byte[][]) _props.get(propName);
            // the array should never be null...
            if (arr.length <= index) {
                // TODO: in case a user sets element 0 and element 90000,
                // we might want to store elements in a hash
                byte[][] newArr = new byte[index + 1][];
                System.arraycopy(arr, 0, newArr, 0, arr.length);
                _props.put(propName, newArr);
                arr = newArr;
            }
            arr[index] = (byte[]) data;

        } else if (data != null) {
            _props.put(propName, data);

        } else {
            _props.remove(propName);
        }
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>turnHolder</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTurnHolder (Name value)
    {
        Name ovalue = this.turnHolder;
        requestAttributeChange(
            TURN_HOLDER, value, ovalue);
        this.turnHolder = value;
    }

    /**
     * Requests that the <code>flashGameService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setFlashGameService (FlashGameMarshaller value)
    {
        FlashGameMarshaller ovalue = this.flashGameService;
        requestAttributeChange(
            FLASH_GAME_SERVICE, value, ovalue);
        this.flashGameService = value;
    }
    // AUTO-GENERATED: METHODS END

    /**
     * A custom serialization method.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();

        // write the number of properties, followed by each one
        out.writeInt(_props.size());
        for (Map.Entry<String, Object> entry : _props.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeObject(entry.getValue());
        }
    }

    /**
     * A custom serialization method.
     */
    public void readObject (ObjectInputStream ins)
        throws IOException, ClassNotFoundException
    {
        ins.defaultReadObject();

        _props.clear();
        int count = ins.readInt();
        while (count-- > 0) {
            String key = ins.readUTF();
            _props.put(key, ins.readObject());
        }
    }

    /** The current state of game data, opaque to us here on the server. */
    protected transient HashMap<String, Object> _props =
        new HashMap<String, Object>();
}
