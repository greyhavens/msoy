//
// $Id$

package com.threerings.msoy.server;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;

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
     * Creates a marshaller for the specified object class.
     */
    public JSONMarshaller (Class<T> pclass)
    {
        _pclass = pclass;
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
                Field field;
                try {
                    field = _pclass.getField(key);
                } catch (NoSuchFieldException e) {
                    continue;
                }

                // the field must be public, non-static and non-transient
                int mods = field.getModifiers();
                if (((mods & Modifier.PUBLIC) == 0) || ((mods & Modifier.STATIC) != 0) ||
                    field.getAnnotation(Transient.class) != null) {
                    continue;
                }
                field.set(obj, state.get(key));
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
            for (Field field : _pclass.getFields()) {
                int mods = field.getModifiers();
                if (((mods & Modifier.PUBLIC) == 0) || ((mods & Modifier.STATIC) != 0) ||
                        field.getAnnotation(Transient.class) != null) {
                    continue;
                }
                state.put(field.getName(), field.get(obj));
            }
            return state.toString().getBytes();
        } catch (Exception e) {
            throw new JSONMarshallingException("Failed to serialize [class=" + _pclass + "]", e);            
        }
    }

    protected Class<T> _pclass;
}
