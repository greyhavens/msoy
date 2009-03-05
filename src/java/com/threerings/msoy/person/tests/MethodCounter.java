package com.threerings.msoy.person.tests;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.HashMap;

import com.google.common.collect.Maps;

/**
 * Simple class to count what methods are called by name only.
 * TODO: support overrides
 */
public class MethodCounter
{
    /**
     * Count the current method call.
     */
    public void count ()
    {
        Throwable t = new Throwable();
        StackTraceElement caller = t.getStackTrace()[1];
        String method = caller.getMethodName();
        Integer count = _counts.get(method);
        if (count == null) {
            count = 1;
        } else {
            count = count + 1;
        }
        _counts.put(method, count);
    }

    /**
     * Gets the number of times a method has been called.
     */
    public int getCount (String methodName)
    {
        Integer count = _counts.get(methodName);
        return count == null ? 0 : count;
    }

    /**
     * Dumps our method counts in the form of a list of asserts for each method in a given class to
     * System.out.
     */
    public void dump (String name, Class<?> clazz)
    {
        dump(System.out, name, clazz);
    }

    /**
     * Dumps our method counts in the form of a list of asserts for each method in a given class.
     */
    public void dump (PrintStream out, String name, Class<?> clazz)
    {
        Method[] methods = clazz.getDeclaredMethods();
        out.println("// Method counts for " + name);
        for (Method method : methods) {
            String mname = method.getName();
            int count = getCount(mname);
            out.println(String.format(
                "assertEquals(%s.getCount(\"%s\"), %d);", name, mname, count));
        }
    }

    protected HashMap<String, Integer> _counts = Maps.newHashMap();
}
