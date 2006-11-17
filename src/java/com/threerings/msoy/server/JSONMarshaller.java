//
// $Id$

package com.threerings.msoy.server;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handles the marshalling and unmarshalling of persistent instances to JSON objects.
 *
 * This format is meant for fairly simple data structures. It will recurse through object
 * fields, but only using their declared class, ignoring runtime class entirely. Do not
 * aim this code at anything that relies on abstract objects or implementing interfaces.
 * Most subclassing will fail. An Object[] array, for example, is useless: its elements
 * will be serialized as Objects, regardless of what they actually are at runtime.
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
     * Returns a JSON marshaller for a given class, either through cache lookup or creation.
     */
    @SuppressWarnings("unchecked")
    public static <T> JSONMarshaller<T> getMarshaller (Class<T> pclass)
    {
        JSONMarshaller<T> marsh = _classMap.get(pclass);
        if (marsh == null) {
            marsh = new JSONMarshaller<T>(pclass);
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
            if ((mods & Modifier.PUBLIC) == 0 || (mods & Modifier.STATIC) != 0 ||
                (mods & Modifier.TRANSIENT) != 0) {
                continue;
            }
            _fields.put(field.getName(), field);
        }
    }

    /**
     * Creates a new instance of the object for which we're responsible, and populates
     * the object with the supplied state, which should be on JSON format.
     */
    public T newInstance (byte[] json)
        throws JSONMarshallingException
    {
        try {
            return deserializeObject(new JSONObject(new String(json)));
        } catch (Exception e) {
            throw new JSONMarshallingException("Failed to deserialize [class=" + _pclass + "]", e);
        }
    }
    
    /**
     * Returns the JSON representation of the state of the given object.
     */
    public byte[] getState (Object obj)
        throws JSONMarshallingException
    {
        try {
            return serialize(obj, obj.getClass()).toString().getBytes();
        } catch (Exception e) {
            throw new JSONMarshallingException("Failed to serialize [class=" + _pclass + "]", e);
        }
    }
    
    // TODO: it's idiotic that org.json does not have its objects implement a JSONValue interface
    protected Object deserialize (Object state, Class<?> dClass)
         throws JSONMarshallingException
    {
        try {
            if (isJSONPrimitive(dClass)) {
                return state;
            }
            if (dClass.isArray()) {
                if (!state.getClass().equals(JSONArray.class)) {
                    throw new JSONMarshallingException(
                        "Can't stuff non-array state into array field [dClass=" + dClass + "]");
                }
                Class cClass = dClass.getComponentType();
                JSONArray jArr = (JSONArray) state;
                int sz = jArr.length();
                Object rArr = Array.newInstance(cClass, sz);
                for (int ii = 0; ii < sz; ii ++ ) {
                    Array.set(rArr, ii, deserialize(jArr.get(ii), cClass));
                }
                return rArr;
            }
            if (!state.getClass().equals(JSONObject.class)) {
                throw new JSONMarshallingException(
                    "Can't stuff non-object state into object field [dClass=" + dClass + "]");
            }
            return getMarshaller(dClass).deserializeObject((JSONObject) state);
        } catch (Exception e) {
            throw new JSONMarshallingException("Failed to deserialize [class=" + _pclass + "]", e);
        }
    }

    protected T deserializeObject (JSONObject jObj)
        throws JSONMarshallingException
    {
        try {
            T obj = _pclass.newInstance();
            Iterator keys = jObj.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                Field field = _fields.get(key);
                if (field == null) {
                    // log? throw exception?
                    continue;
                }
                field.set(obj, deserialize(jObj.get(key), field.getType()));
            }
            return obj;
        } catch (Exception e) {
            throw new JSONMarshallingException("Failed to deserialize [class=" + _pclass + "]", e);
        }
    }

    // TODO: it's idiotic that org.json does not have its objects implement a JSONValue interface
    protected Object serialize (Object value, Class<?> dClass)
        throws JSONException, IllegalAccessException
    {
        if (isJSONPrimitive(dClass)) {
            return value;
        }
        if (dClass.isArray()) {
            Class cClass = dClass.getComponentType();
            int sz = Array.getLength(value);
            JSONArray jArr = new JSONArray();
            for (int ii = 0; ii < sz; ii ++ ) {
                jArr.put(serialize(Array.get(value, ii), cClass));
            }
            return jArr;
        }
        if (!_pclass.equals(dClass)) {
            return getMarshaller(dClass).serialize(value, dClass);
        }
        JSONObject state = new JSONObject();
        for (Field field : _fields.values()) {
            state.put(field.getName(), serialize(field.get(value), field.getType()));
        }
        return state;
    }
    
    // convenience method for categorizing anything JSON treats as primitive
    protected boolean isJSONPrimitive(Class vClass) {
        return (vClass.equals(Boolean.TYPE) || vClass.equals(Boolean.class) ||
                vClass.equals(Byte.TYPE) || vClass.equals(Byte.class) ||
                vClass.equals(Short.TYPE) || vClass.equals(Short.class) ||
                vClass.equals(Integer.TYPE) || vClass.equals(Integer.class) ||
                vClass.equals(Long.TYPE) || vClass.equals(Long.class) ||
                vClass.equals(Float.TYPE) || vClass.equals(Float.class) ||
                vClass.equals(Double.TYPE) || vClass.equals(Double.class) ||
                vClass.equals(String.class));
    }

    /** The class for whom we're marshalling. */
    protected Class<T> _pclass;
    /** Names of public, non-static, non-transient fields mapped to {@link Field} instances. */
    protected Map<String, Field> _fields = new HashMap<String, Field>();

    /** The static cache of instantiated marshallers. */
    protected static Map<Class, JSONMarshaller> _classMap = new HashMap<Class, JSONMarshaller>();
}
