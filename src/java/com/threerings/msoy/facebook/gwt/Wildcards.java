package com.threerings.msoy.facebook.gwt;

import java.util.Map;

public class Wildcards
{
    public static String replace (String original, Map<String, String> replacements)
    {
        return new Wildcards(replacements).replace(original);
    }

    public Wildcards (Map<String, String> replacements)
    {
        _replacements = replacements;
    }

    public String replace (String original)
    {
        String replaced = original;
        for (Map.Entry<String, String> pair : _replacements.entrySet()) {
            if (pair.getValue() == null) {
                continue;
            }
            String key = "{*" + pair.getKey() + "*}";
            replaced = replaced.replace(key, pair.getValue());
        }
        return replaced;
    }

    protected Map<String, String> _replacements;
}
