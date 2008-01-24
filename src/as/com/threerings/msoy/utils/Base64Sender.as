//
// $Id$

package com.threerings.msoy.utils {

import flash.events.TimerEvent;

import flash.external.ExternalInterface;

import flash.utils.ByteArray;
import flash.utils.Timer;

import com.threerings.util.Log;

// TODO: share constants between Sender and Receiver?
// ".start" and ".end"?
public class Base64Sender
{
    public function Base64Sender (
        externalFunctionName :String = "setBytes", maxChunkSize :int = 81920)
    {
        _funcName = externalFunctionName;
        _maxChunkSize = maxChunkSize;
        _timer = new Timer(1); // fire every frame
        _timer.addEventListener(TimerEvent.TIMER, doChunk);
    }

    /**
     * Send the specified ByteArray over javascript on the function name specified
     * in the constructor.
     *
     * Note that the send may happen asynchronously, so do not modify the byte array.
     */
    public function sendBytes (bytes :ByteArray) :void
    {
        // see if we're interrupting a previous send
        if (_bytes != null) {
            send(".reset");
            _timer.reset();
        }

        _bytes = bytes;
        _position = 0;
        doChunk();
    }

    protected function doChunk (... ignored) :void
    {
        var length :int = Math.min(_bytes.length - _position, _maxChunkSize);
        if (length > 0) {
            var encoder :Base64Encoder = new Base64Encoder();
            encoder.insertNewLines = false;
            encoder.encodeBytes(_bytes, _position, length);
            send(encoder.toString());
            _position += length;
        }

        if (_position == _bytes.length) {
            // we're done sending
            _bytes = null;
            send(null);
            _timer.reset();

        } else {
            // there is more to send!
            _timer.start(); // no effect if already started...
        }
    }

    protected function send (s :String) :void
    {
        try {
            ExternalInterface.call(_funcName, s);

        } catch (err :Error) {
            Log.getLog(this).logStackTrace(err);
        }
    }

    protected var _position :int;

    protected var _bytes :ByteArray;

    protected var _funcName :String;

    protected var _maxChunkSize :int;

    protected var _timer :Timer;
}
}
