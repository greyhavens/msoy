package com.threerings.msoy.swiftly.client;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ActionScriptTokens
{
    public final Set<String> reserved;
    public final Set<String> types;

    ActionScriptTokens ()
    {
        reserved = new HashSet<String>();
        // TODO not complete
        reserved.addAll(Arrays.asList(new String[] {
         "public", "private", "internal", "protected", "override", "final", "dynamic", "native", 
         "static", "const", "var", "class", "extends", "interface", "implements", "package",
          "namespace", "function", "function get", "function set", "import", "include", "for",
          "for each", "super", "this"
        }));

        types = new HashSet<String>();
        // TODO not complete
        types.addAll(Arrays.asList(new String[] {
         "Object", "Function", "Array", "String", "Boolean", "Number", "Date", "Error", "RegExp", 
         "XML", "int", "uint", "void", "*"
        }));
    }
}
