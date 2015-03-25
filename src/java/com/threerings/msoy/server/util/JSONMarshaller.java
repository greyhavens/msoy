//
// $Id$

package com.threerings.msoy.server.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.Maps;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.samskivert.util.ByteEnum;
import com.samskivert.util.ByteEnumUtil;

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
     * Used to migrate old data to new. Currently only supports field renaming.
     */
    public static interface Migration
    {
        /** Converts an old field to its new name. */
        String migrateField (String original);
    }

    public static interface JSONMutator<T>
    {
        public T jsonMutate (T obj);
    }

    /**
     * Registers a migration for a particular JSON encoded class.
     */
    public static void registerMigration (Class<?> pclass, Map<String,String> migration)
    {
        _migrations.put(pclass, migration);
    }

    /**
     * Registers a mutatoin for a particular JSON encoded class.
     */
    public static <T> void registerMutator (Class<T> pclass, JSONMutator<T> mutator)
    {
        _mutators.put(pclass, mutator);
    }

    /**
     * Returns a JSON marshaller for a given class, either through cache lookup or creation.
     */
    public static <T> JSONMarshaller<T> getMarshaller (Class<T> pclass)
    {
        @SuppressWarnings("unchecked") JSONMarshaller<T> marsh =
            (JSONMarshaller<T>)_classMap.get(pclass);
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
        _migmap = _migrations.get(pclass);

        @SuppressWarnings("unchecked")
            JSONMutator<T> mutator = (JSONMutator<T>) _mutators.get(pclass);
        _mutator = mutator;
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
    public byte[] getStateBytes (Object obj)
        throws JSONMarshallingException
    {
        try {
            return serialize(obj, obj.getClass()).toString().getBytes();

        } catch (Exception e) {
            throw new JSONMarshallingException("Failed to serialize [class=" + _pclass + "]", e);
        }
    }

    /**
     * Serialize a non-primitive non-array into a JSONObject and return it.
     */
    public JSONObject getStateObject (Object obj)
        throws JSONMarshallingException
    {
        if (!obj.getClass().equals(_pclass)) {
            throw new JSONMarshallingException(
                "Object class doesn't match marshaller [class=" + obj.getClass() + "]");
        }
        try {
            return serializeObject(obj);

        } catch (Exception e) {
            throw new JSONMarshallingException("Failed to serialize [class=" + _pclass + "]", e);
        }
    }

    // TODO: it's idiotic that org.json does not have its objects implement a JSONValue interface
    protected Object deserialize (Object state, Class<?> dClass)
         throws JSONMarshallingException
    {
        try {
            // try the exhaustive list of possible JSON types
            if (state instanceof String || state instanceof Boolean) {
                return state;
            }
            if (state instanceof Integer) {
                if (dClass.equals(Byte.class) || dClass.equals(Byte.TYPE)) {
                    // try to cram a JSON integer into a byte
                    return ((Integer) state).byteValue();
                }
                if (dClass.equals(Short.class) || dClass.equals(Short.TYPE)) {
                    // try to cram a JSON integer into a short
                    return ((Integer) state).shortValue();
                }
                if (dClass.isEnum()) {
                    if (!ByteEnum.class.isAssignableFrom(dClass)) {
                        throw new JSONMarshallingException(
                            "Can't deserialize enum that's not ByteEnum [class=" + _pclass + "]");
                    }

                    @SuppressWarnings("unchecked")
                    Class<EnumReader> eClass = (Class<EnumReader>)dClass;

                    return ByteEnumUtil.fromByte(eClass, ((Integer) state).byteValue());
                }
                return state;
            }
            if (state instanceof Long) {
                // if the field can't handle it our caller will deal
                return state;
            }
            if (state instanceof Double) {
                if (dClass.equals(Float.class) || dClass.equals(Float.TYPE)) {
                    return ((Double) state).floatValue();
                }
                return state;
            }
            if (state instanceof JSONArray) {
                if (!dClass.isArray()) {
                    throw new JSONMarshallingException(
                        "Can't stuff non-array state into array field [dClass=" + dClass + "]");
                }
                Class<?> cClass = dClass.getComponentType();
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
            @SuppressWarnings("unchecked") Iterator<String> keys = jObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String mkey = _migmap != null ? _migmap.get(key) : null;
                Field field = _fields.get(mkey == null ? key : mkey);
                if (field == null) {
                    // log? throw exception?
                    continue;
                }
                Object value = deserialize(jObj.get(key), field.getType());
                field.set(obj, value);
            }

            if (_mutator != null) {
                T old = obj;
                obj = _mutator.jsonMutate(obj);
                com.threerings.msoy.Log.log.info("Mutated object", "before", old, "after", obj);
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
        if (value == null) {
            return null;
        }
        if (isJSONPrimitive(dClass)) {
            return value;
        }
        if (dClass.isArray()) {
            Class<?> cClass = dClass.getComponentType();
            int sz = Array.getLength(value);
            JSONArray jArr = new JSONArray();
            for (int ii = 0; ii < sz; ii ++ ) {
                jArr.put(serialize(Array.get(value, ii), cClass));
            }
            return jArr;
        }
        if (dClass.isEnum()) {
            if (!ByteEnum.class.isAssignableFrom(dClass)) {
                throw new JSONException(
                    "Can't serialize enum that's not ByteEnum [class=" + _pclass + "]");
            }
            return ((ByteEnum) value).toByte();
        }
        return getMarshaller(dClass).serializeObject(value);
    }

    protected JSONObject serializeObject (Object value)
        throws JSONException, IllegalAccessException
    {
        JSONObject state = new JSONObject();
        for (Field field : _fields.values()) {
            state.put(field.getName(), serialize(field.get(value), field.getType()));
        }
        return state;
    }

    // convenience method for categorizing anything JSON treats as primitive
    protected boolean isJSONPrimitive(Class<?> vClass) {
        return (vClass.equals(Boolean.TYPE) || vClass.equals(Boolean.class) ||
                vClass.equals(Byte.TYPE) || vClass.equals(Byte.class) ||
                vClass.equals(Short.TYPE) || vClass.equals(Short.class) ||
                vClass.equals(Integer.TYPE) || vClass.equals(Integer.class) ||
                vClass.equals(Long.TYPE) || vClass.equals(Long.class) ||
                vClass.equals(Float.TYPE) || vClass.equals(Float.class) ||
                vClass.equals(Double.TYPE) || vClass.equals(Double.class) ||
                vClass.equals(String.class));
    }

    /** Used to coerce the type system into quietude when reading enums from the wire. */
    protected static enum EnumReader implements ByteEnum {
        NOT_USED;
        public byte toByte () { return 0; }
    }

    /** The class for whom we're marshalling. */
    protected Class<T> _pclass;

    /** The mutator for the class we're marshalling or null. */
    protected JSONMutator<T> _mutator;

    /** The migration map for the class we're marshalling or null. */
    protected Map<String, String> _migmap;

    /** Names of public, non-static, non-transient fields mapped to {@link Field} instances. */
    protected Map<String, Field> _fields = Maps.newHashMap();

    /** The static cache of instantiated marshallers. */
    protected static Map<Class<?>, JSONMarshaller<?>> _classMap = Maps.newHashMap();

    /** The static registry of migrations. */
    protected static Map<Class<?>, Map<String, String>> _migrations = Maps.newHashMap();

    /** Some classes need to be mutated into something else when they come off the wire. */
    protected static Map<Class<?>, JSONMutator<?>> _mutators = Maps.newHashMap();
}
