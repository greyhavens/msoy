//
// $Id: $


package com.threerings.msoy.admin.config.server;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.threerings.admin.server.ConfigRegistry;
import com.threerings.presents.dobj.DObject;

import com.threerings.msoy.admin.config.gwt.ConfigField;
import com.threerings.msoy.admin.config.gwt.ConfigField.FieldType;
import com.threerings.msoy.admin.config.gwt.ConfigService;

import com.threerings.msoy.admin.data.MsoyAdminCodes;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.google.inject.Inject;

import static com.threerings.msoy.Log.log;

/**
 *
 */
public class ConfigServlet extends MsoyServiceServlet
    implements ConfigService
{
    @Override public ConfigurationResult getConfig ()
        throws ServiceException
    {
        requireAdminUser();

        Map<String, List<ConfigField>> tabs = Maps.newHashMap();
        for (String key : _confReg.getKeys()) {
            DObject object = _confReg.getObject(key);

            List<ConfigField> configFields = Lists.newArrayList();
            Field[] fields = object.getClass().getFields();
            for (Field field : fields) {
                if (field.getModifiers() != Modifier.PUBLIC) {
                    continue;
                }
                FieldType type = TYPES.get(field.getType());
                if (type == null) {
                    log.warning("Unknown field type", "field", field.getName(),
                                "type", field.getType());
                    throw new ServiceException(MsoyAdminCodes.E_INTERNAL_ERROR);
                }
                try {
                    Object value = field.get(object);
                    String valStr = (value != null) ? value.toString() : null;
                    configFields.add(new ConfigField(field.getName(), type, valStr));

                } catch (IllegalAccessException e) {
                    log.warning("Failure reflecting on configuration object", "key", key,
                        "object", object, "field", field, e);
                    throw new ServiceException(MsoyAdminCodes.E_INTERNAL_ERROR);
                }
            }
            tabs.put(key, configFields);
        }

        ConfigurationResult result = new ConfigurationResult();
        result.records = tabs;
        return result;
    }

    @Override public ConfigurationResult updateConfiguration (List<ConfigField> updates)
    {
        return null;
    }

    @Inject protected ConfigRegistry _confReg;

    protected static Map<Class<?>, FieldType> TYPES = ImmutableMap.<Class<?>, FieldType>builder()
        .put(Integer.class, FieldType.INTEGER)
        .put(Integer.TYPE, FieldType.INTEGER)
        .put(Short.class, FieldType.SHORT)
        .put(Short.TYPE, FieldType.SHORT)
        .put(Long.class, FieldType.LONG)
        .put(Long.TYPE, FieldType.LONG)
        .put(Float.class, FieldType.FLOAT)
        .put(Float.TYPE, FieldType.FLOAT)
        .put(Double.class, FieldType.DOUBLE)
        .put(Double.TYPE, FieldType.DOUBLE)
        .put(Boolean.class, FieldType.BOOLEAN)
        .put(Boolean.TYPE, FieldType.BOOLEAN)
        .put(String.class, FieldType.STRING)
        .build();
}
