//
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.client.InvocationAdapter;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.dobj.DObject;
import com.threerings.util.Integer;
import com.threerings.util.Log;
import com.threerings.util.ObjectMarshaller;
import com.whirled.game.client.PropertySpaceHelper;
import com.whirled.game.data.PropertySetEvent;
import com.whirled.game.data.PropertySetAdapter;
import com.whirled.game.data.PropertySpaceObject;

/**
 * Various functions useful to both the thane and the flash backends.
 */
public class BackendUtils
{
    public static const log :Log = Log.getLog(BackendUtils);

    /**
     * Throws an error if the name is not a valid property name.
     */
    public static function validateName (name :String) :void
    {
        if (name == null) {
            throw new ArgumentError("Property, message, and collection names must not be null.");
        }
    }

    /**
     * Verify that the value is legal to be streamed to other clients.
     */
    public static function validateValue (value :Object) :void
    {
        ObjectMarshaller.validateValue(value);
    }

    /**
     * Verify that the property name / value are valid.
     */
    public static function validatePropertyChange (
        propName :String, value :Object, array :Boolean, index :Object) :void
    {
        validateName(propName);

        if (array) {
            if (index == null || int(index) < 0) {
                throw new ArgumentError("Bogus array index specified.");
            }
            // TODO: fixy
//            if (!(_gameData[propName] is Array)) {
//                throw new ArgumentError("Property " + propName + " is not an Array.");
//            }
        }

        // validate the value too
        validateValue(value);
    }

    /**
     * Performs a standard property set.
     */
    public static function encodeAndSet (
        client :Client, obj :PropertySpaceObject, name :String, value :Object, key :Object, 
        isArray :Boolean, immediate :Boolean) :void
    {
        validatePropertyChange(name, value, isArray, key);

        var encoded :Object = PropertySpaceHelper.encodeProperty(value, (key == null));
        var ikey :Integer = (key == null) ? null : new Integer(int(key));

        // TODO: remove this check
        if (obj.getPropService() == null) {
            log.info("No property service for " + obj);
            return;
        }

        obj.getPropService().setProperty(
            client, name, encoded, ikey, isArray, false, null, 
            loggingConfirmListener("setProperty"));

        if (immediate) {
            // we re-decode so that it looks like it came off the net
            // TODO: fix broken behaviour
            try {
                PropertySpaceHelper.applyPropertySet(
                    obj, name, PropertySpaceHelper.decodeProperty(encoded),
                    key, isArray);

            } catch (re :RangeError) {
                trace("Error setting property (immediate): " + re);
            }
        }
    }

    /**
     * Creates a confirm listener that will log failures with a service name and optionally 
     * call a function on success.
     */
    public static function loggingConfirmListener (svc :String, processed :Function = null)
        :InvocationService_ConfirmListener
    {
        return new ConfirmAdapter(function (cause :String) :void {
            log.warning("Service failure [service=" + svc + ", cause=" + cause + "].");
        }, processed);
    }

    /**
     * Creates an invocation listener that will log failures with a service name.
     */
    public static function loggingInvocationListener (svc :String) 
        :InvocationService_InvocationListener
    {
        return new InvocationAdapter(function (cause :String) :void {
            log.warning("Service failure [service=" + svc + ", cause=" + cause + "].");
        });
    }
}
}
