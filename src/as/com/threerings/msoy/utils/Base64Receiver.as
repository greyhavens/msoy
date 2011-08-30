//
// $Id$

package com.threerings.msoy.utils {

import flash.external.ExternalInterface;
import flash.utils.ByteArray;

/**
 * Receives byte packets over javascript and calls a method when done.
 */
public class Base64Receiver
{
    /**
     * Create a Base64Receiver to receive bytes on the specified external function name.
     *
     * @param resultFn signature: function (ByteArray) :void
     * @param externalFunctionName the name of the function to expose to javascript. Default is
     *        "setBytes".
     *
     * @throws an Error if the callback cannot be set up on the ExternalInterface.
     */
    public function Base64Receiver (resultFn :Function, externalFunctionName :String = "setBytes")
    {
        if (ExternalInterface.available) {
            ExternalInterface.addCallback(externalFunctionName, receiveBytes);
        } else {
            throw new Error("Cannot configure a Base64Receiver: ExternalInterface not available.");
        }
        _resultFn = resultFn;
    }

    /**
     * This method is exposed to javascript.
     */
    protected function receiveBytes (base64Encoded :String) :Boolean
    {
        // a null indicates that sending is done
        if (base64Encoded == null) {
            var ba :ByteArray;
            if (_decoder != null) {
                ba = _decoder.toByteArray();
                _decoder = null;

            } else {
                ba = new ByteArray(); // we were sent 0 bytes...
            }
            _resultFn(ba);

        } else if (base64Encoded == ".reset") {
            _decoder = null;

        } else {
            // possibly append this chunk to bytes we're already received
            if (_decoder == null) {
                _decoder = new Base64Decoder();
            }
            _decoder.decode(base64Encoded);
        }

        return true; // indicate to the receiver that we got the bytes
    }

    /** The function to call when we have a result. */
    protected var _resultFn :Function;

    /** The decoder, non-null only while we're receiving data. */
    protected var _decoder :Base64Decoder;
}
}
