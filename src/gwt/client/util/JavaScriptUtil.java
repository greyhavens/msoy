//
// $Id$

package client.util;

import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Handles converting to and from elements of a JavaScript array.
 */
public class JavaScriptUtil
{
    /**
     * Returns the length of the supplied array.
     */
    public static native int getLength (JavaScriptObject array) /*-{
        return array.length;
    }-*/;

    /**
     * Extracts a sub-array element from the supplied array.
     */
    public static native JavaScriptObject getJavaScriptElement (
        JavaScriptObject array, int index) /*-{
        return array[index];
    }-*/;

    /**
     * Extracts a string element from the supplied array.
     */
    public static native String getStringElement (JavaScriptObject array, int index) /*-{
        return array[index];
    }-*/;

    /**
     * Extracts an integer element from the supplied array.
     */
    public static native int getIntElement (JavaScriptObject array, int index) /*-{
        return array[index];
    }-*/;

    /**
     * Extracts a byte element from the supplied array.
     */
    public static native byte getByteElement (JavaScriptObject array, int index) /*-{
        return array[index];
    }-*/;

    /**
     * Extracts a boolean element from the supplied array.
     */
    public static native boolean getBooleanElement (JavaScriptObject array, int index) /*-{
        return array[index];
    }-*/;

    /**
     * Sets a string element into the supplied array.
     */
    public static native String setStringElement (
        JavaScriptObject array, int index, String value) /*-{
        array[index] = value;
    }-*/;

    /**
     * Sets an integer element into the supplied array.
     */
    public static native int setIntElement (JavaScriptObject array, int index, int value) /*-{
        array[index] = value;
    }-*/;

    /**
     * Sets a byte element into the supplied array.
     */
    public static native byte setByteElement (JavaScriptObject array, int index, byte value) /*-{
        array[index] = value;
    }-*/;

    /**
     * Sets a boolean element into the supplied array.
     */
    public static native boolean setBooleanElement (
        JavaScriptObject array, int index, boolean value) /*-{
        array[index] = value;
    }-*/;

    /**
     * Converts a java map into a javascript dictionary that can then be passed into a native
     * method. Returns null in the event of an exception and logs the error to the console, if
     * active. Note that each value must be a JSNI type (String, boolean, JavaScriptObject or a
     * numeric type other than long). As far as I'm aware, the map key type must be String. 
     */
    public static native <K, V> JavaScriptObject createDictionaryFromMap (Map<K, V> map) /*-{
        try {
            var obj = {};
            var entries = map.@java.util.Map::entrySet()();
            var iter = entries.@java.util.Set::iterator()();
            while (iter.@java.util.Iterator::hasNext()()) {
                var entry = iter.@java.util.Iterator::next()();
                var key = entry.@java.util.Map.Entry::getKey()();
                var value = entry.@java.util.Map.Entry::getValue()();
                obj[key] = value;
            }

        } catch (e) {
            if ($wnd.console) {
                $wnd.console.log("Error in createFromMap: " + e);
            }
            return null;
        }
        return obj;
    }-*/;

    /**
     * Attempts to retrieve an entry from a java script dictionary. Returns null in the event of
     * an exception and logs the error to the console, if active.
     */
    public static native <K, V> V getDictionaryEntry (JavaScriptObject map, K key) /*-{
        try {
            return "" + map[key];

        } catch (e) {
            if ($wnd.console) {
                $wnd.console.log("Error in getMapEntry: " + e);
            }
            return null;
        }
    }-*/;

    /**
     * Converts a javascript dictionary to a string for debugging. Returns null if there is an
     * exception and logs the exception to the console if available.
     */
    public static native String dictionaryToString (JavaScriptObject map) /*-{
        try {
            var s = "{";
            for (var key in map) {
                s += "\"" + key + "\": \"" + map[key] + "\", ";
            }
            s += "}";
            return s;

        } catch (e) {
            if ($wnd.console) {
                $wnd.console.log("Error in dictionaryToString: " + e);
            }
            return null;
        }
    }-*/;

    /**
     * Creates a javascript array containing the given elements. Note that each element must be a
     * JSNI type (String, boolean, JavaScriptObject or a numeric type other than long). Returns
     * null in the event of an exception and logs the error to the console, if active.
     */
    public static native JavaScriptObject createArray (List<Object> elements) /*-{
        try {
            var array = [];
            var iter = elements.@java.util.List::iterator()();
            while (iter.@java.util.Iterator::hasNext()()) {
                var elem = iter.@java.util.Iterator::next()();
                array.push(elem);
            }
            return array;
        } catch (e) {
            if ($wnd.console) {
                $wnd.console.log("Error in createArray: " + e);
            }
            return null;
        }
    }-*/;
}
