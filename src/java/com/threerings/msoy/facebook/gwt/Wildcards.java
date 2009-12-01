//
// $Id$

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
        StringBuilder output = new StringBuilder();
        int pos = 0;
        while (true) {
            int kstart = original.indexOf("{*", pos);
            if (kstart == -1) {
                output.append(original, pos, original.length());
                break;
            }
            output.append(original, pos, kstart);
            int kend = original.indexOf("*}", kstart);
            appendReplacement(output, original.substring(kstart + 2, kend));
            pos = kend + 2;
        }
        return output.toString();
    }

    protected void appendReplacement (StringBuilder output, String key)
    {
        String repl = _replacements.get(key);
        if (repl != null) {
            output.append(repl);
            return;
        }
        output.append("{*").append(key).append("*}");
    }

    protected Map<String, String> _replacements;
}
