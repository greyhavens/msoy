//
// $Id$

package com.threerings.msoy.utils;

import java.applet.Applet;

import com.samskivert.util.Interval;
import com.samskivert.util.RunQueue;

import netscape.javascript.JSObject;

import org.apache.commons.codec.binary.Base64;

public class Base64Sender
{
    public Base64Sender (Applet containingApplet, String targetName)
    {
        this(containingApplet, targetName, "setBytes");
    }

    public Base64Sender (Applet containingApplet, String targetName, String functionName)
    {
        this(containingApplet, targetName, functionName, 81920, 10);
    }

    public Base64Sender (
        Applet containingApplet, String targetName, String functionName,
        int maxChunkSize, int chunksPerSecond)
    {
        JSObject win = JSObject.getWindow(containingApplet);
        JSObject doc = (JSObject) win.getMember("document");
        _target = (JSObject) doc.getMember(targetName);

        if (_target == null) {
            throw new IllegalArgumentException("Unable to find target: " + targetName);
        }

        _funcName = functionName;
        _maxChunkSize = maxChunkSize * 4 / 3; // convert pre-encoded byte chunk size to post-size
        _chunksPerSecond = chunksPerSecond;

        _interval = new Interval(RunQueue.AWT) {
            public void expired () {
                doChunk();
            }
        };
    }

    public void sendBytes (byte[] bytes)
    {
        if (_bytes != null) {
            _interval.cancel();
            send(".reset");
        }

        _bytes = Base64.encodeBase64(bytes);
        _position = 0;
        if (doChunk()) {
            _interval.schedule(1000 / _chunksPerSecond);
        }
    }

    /**
     * Do a chunk. Return true if there's more to do.
     */
    protected boolean doChunk ()
    {
        int length = Math.min(_bytes.length - _position, _maxChunkSize);
        if (length > 0) {
            send(new String(_bytes, _position, length));
            _position += length;
        }

        if (_position == _bytes.length) {
            // we're done sending
            _bytes = null;
            send(null);
            _interval.cancel();
            return false;
        }

        return true; // more to send
    }

    protected void send (String s)
    {
        _target.call(_funcName, new Object[] { s });
    }

    protected int _position;

    protected byte[] _bytes;

    protected String _funcName;

    protected JSObject _target;

    protected int _maxChunkSize;

    protected int _chunksPerSecond;

    protected Interval _interval;
}
