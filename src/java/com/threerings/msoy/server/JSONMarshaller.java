//
// $Id$

package com.threerings.msoy.server;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.persistence.Transient;

import org.json.JSONObject;

/**
 * Handles the marshalling and unmarshalling of persistent instances to JSON objects.
 */
public class JSONMarshaller<T>
{
    /**
     * Used to flag an error in the JSON marshalling/unmarshalling system.
     */
    public static class JSONMarshallingException extends Exception
    {
        public JSONMarshallingException ()
        {
            super();
        }

        public JSONMarshallingException (String message, Throwable cause)
        {
            super(message, cause);
        }

        public JSONMarshallingException (String message)
        {
            super(message);
        }
    }

    /**
     * Return a JSON marshaller for a given class, either through cache lookup or creation.
     */
    public static <T> JSONMarshaller getMarshaller (Class<T> pclass)
    {
        JSONMarshaller marsh = _classMap.get(pclass);
        if (marsh == null) {
            marsh = new JSONMarshaller(pclass);
            _classMap.put(pclass, marsh);
        }
        return marsh;
    }
    
    /**
     * Creates a marshaller for the specified object class.
     */
    protected JSONMarshaller (Class<T> pclass)
    {
        _pclass = pclass;
        for (Field field : pclass.getFields()) {
            int mods = field.getModifiers();
            if (((mods & Modifier.PUBLIC) == 0) || ((mods & Modifier.STATIC) != 0) ||
                field.getAnnotation(Transient.class) != null) {
                continue;
            }
            _fields.put(field.getName(), field);
        }
    }

    /**
     * Creates a new instance of the object for which we're responsible, and populates
     * the object with the supplied state string, which should be on JSON format.
     */
    public T newInstance (byte[] json)
        throws JSONMarshallingException
    {
        try {
            JSONObject state = new JSONObject(new String(json));
            T obj = _pclass.newInstance();
            Iterator keys = state.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                Field field = _fields.get(key);
                if (field != null) {
                    field.set(obj, state.get(key));
                }
            }
            return obj;
        } catch (Exception e) {
            throw new JSONMarshallingException("Failed to deserialize [class=" + _pclass + "]", e);
        }
    }

    /**
     * Returns the JSON representation of the given object's state.
     */
    public byte[] getState (T obj)
        throws JSONMarshallingException
    {
        try {
            JSONObject state = new JSONObject();
            for (Field field : _fields.values()) {
                state.put(field.getName(), field.get(obj));
            }
            return state.toString().getBytes();
        } catch (Exception e) {
            throw new JSONMarshallingException("Failed to serialize [class=" + _pclass + "]", e);            
        }
    }

    protected Map<String, Field> _fields = new HashMap<String, Field>();
    protected Class<T> _pclass;

    protected static Map<Class, JSONMarshaller> _classMap = new HashMap<Class, JSONMarshaller>();
}
