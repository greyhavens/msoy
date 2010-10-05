//
// $Id: $


package com.threerings.msoy.admin.config.gwt;

import com.google.common.collect.ComparisonChain;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 *
 */
public class ConfigField
    implements IsSerializable, Comparable<ConfigField>
{
    public enum FieldType
        implements IsSerializable
    {
        INTEGER,
        SHORT,
        BYTE,
        LONG,
        FLOAT,
        BOOLEAN,
        DOUBLE,
        STRING
    }

    public String name;
    public FieldType type;
    public String valStr;

    public ConfigField ()
    {
    }

    public ConfigField (String name, FieldType type, String valStr)
    {
        this.name = name;
        this.type = type;
        this.valStr = valStr;
    }

    public Object toValue (String text)
    {
        switch(type) {
        case INTEGER:
            return new Integer(text);
        case SHORT:
            return new Short(text);
        case BYTE:
            return new Byte(text);
        case LONG:
            return new Long(text);
        case FLOAT:
            return new Float(text);
        case DOUBLE:
            return new Double(text);
        case BOOLEAN:
            return new Boolean(text);
        case STRING:
            return text;
        }
        return null;
    }


    public int compareTo (ConfigField o)
    {
        return ComparisonChain.start().compare(name, o.name).result();
    }

    @Override
    public boolean equals (Object o)
    {
        return compareTo((ConfigField) o) == 0;
    }

    @Override
    public int hashCode ()
    {
        return name.hashCode();
    }
}
