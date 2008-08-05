//
// $Id$

package client.util;

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
}
