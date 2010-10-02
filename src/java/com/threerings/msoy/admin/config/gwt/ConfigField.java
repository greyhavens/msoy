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
    public enum FieldType {
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
    public Object value;

    public ConfigField ()
    {
    }

    public ConfigField (String name, FieldType type, Object value)
    {
        this.name = name;
        this.type = type;
        this.value = value;
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
