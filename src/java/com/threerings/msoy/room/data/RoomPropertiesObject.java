package com.threerings.msoy.room.data;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.presents.dobj.DObject;
import com.whirled.game.data.PropertySpaceMarshaller;
import com.whirled.game.data.PropertySpaceObject;
import com.whirled.game.data.WhirledGameMessageMarshaller;
import com.whirled.game.server.PropertySpaceHelper;

/**
 * Provides a property space for a room.
 */
public class RoomPropertiesObject extends DObject
    implements PropertySpaceObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>propertiesService</code> field. */
    public static final String PROPERTIES_SERVICE = "propertiesService";

    /** The field name of the <code>messageService</code> field. */
    public static final String MESSAGE_SERVICE = "messageService";
    // AUTO-GENERATED: FIELDS END

    /** Service for setting the properties. */
    public PropertySpaceMarshaller propertiesService;
    
    /** Service for sending messages to the room occupants (that are also playing the game that 
     * these properties belong to). */
    public WhirledGameMessageMarshaller messageService;
    
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

    /**
     * The current state of game data for a room.
     * On the server, this will be a byte[] for normal properties and a byte[][] for array
     * properties. On the client, the actual values are kept whole.
     */
    protected transient HashMap<String, Object> _props = new HashMap<String, Object>();

    /**
     * The persistent properties that have been written to since startup.
     */
    protected transient Set<String> _dirty = new HashSet<String>();

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>propertiesService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPropertiesService (PropertySpaceMarshaller value)
    {
        PropertySpaceMarshaller ovalue = this.propertiesService;
        requestAttributeChange(
            PROPERTIES_SERVICE, value, ovalue);
        this.propertiesService = value;
    }

    /**
     * Requests that the <code>messageService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setMessageService (WhirledGameMessageMarshaller value)
    {
        WhirledGameMessageMarshaller ovalue = this.messageService;
        requestAttributeChange(
            MESSAGE_SERVICE, value, ovalue);
        this.messageService = value;
    }
    // AUTO-GENERATED: METHODS END
}
