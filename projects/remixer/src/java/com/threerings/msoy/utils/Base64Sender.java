//
// $Id$

package com.threerings.msoy.utils;

import java.applet.Applet;

import com.samskivert.util.BasicRunQueue;
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
        _applet = containingApplet;
        _targetName = targetName;

//        BasicRunQueue runQ = new BasicRunQueue();
//        runQ.setDaemon(true);
//        runQ.start();
//        _runQueue = runQ;
        _runQueue = RunQueue.AWT;

        _funcName = functionName;
        _maxChunkSize = maxChunkSize * 4 / 3; // convert pre-encoded byte chunk size to post-size
        _chunksPerSecond = chunksPerSecond;

        _interval = new Interval(_runQueue) {
            public void expired () {
                if (!doChunk()) {
                    cancel(); // this interval
                }
            }
        };
    }

    public void sendBytes (final byte[] bytes)
    {
        _runQueue.postRunnable(new Runnable() {
            public void run () {
                if (_bytes != null) {
                    _interval.cancel();
                    send(".reset");
                }

                _bytes = Base64.encodeBase64(bytes);
                _position = 0;
                _failures = 0;
                if (doChunk()) {
                    _interval.schedule(1000 / _chunksPerSecond, true);
                }
            }
        });
    }

    /**
     * Do a chunk. Return true if there's more to do.
     */
    protected boolean doChunk ()
    {
        int length = Math.min(_bytes.length - _position, _maxChunkSize);
        if (length > 0) {
            if (send(new String(_bytes, _position, length))) {
                _position += length;

            } else {
                // we did not succeed in sending this chunk..
                System.err.println("Did not send. Waiting...");
                if (++_failures >= _maxFailures) {
                    System.err.println("Too many failures. Giving up.");
                    _bytes = null;
                    return false;
                }
                // else, fall through: return true
            }
        }

        if (_position == _bytes.length) {
            // we're done sending
            send(null); // we don't check this, the assumption is that if the last chunk worked,
                        // this will too.
            _bytes = null;
            return false;
        }

        return true; // more to send
    }

    protected boolean send (String s)
    {
        JSObject win = JSObject.getWindow(_applet);
        if (win == null) {
            System.err.print("Could not find window! "); // just print,
            return false;
        }
        JSObject doc = (JSObject) win.getMember("document");
        if (doc == null) {
            System.err.print("Could not find document! "); // just print,
            return false;
        }
        JSObject target = (JSObject) doc.eval("getElementById('" + _targetName + "');");
        if (target == null) {
            System.err.print("Could not find target! "); // just print,
//            System.err.println("Can't find target.");
            return false;
        }

        Object resultValue = target.call("setMediaBytes", new Object[] { s });
        boolean result = Boolean.TRUE.equals(resultValue);
        if (result) {
            if (s == null) {
                System.err.println("Sent a null.");
            } else {
                System.err.println("Sent a String (" + s.length() + ")");
            }
        }

        return result;
    }

    protected RunQueue _runQueue;

    protected int _position;

    protected byte[] _bytes;

    protected String _targetName;

    protected String _funcName;

    protected int _maxChunkSize;

    protected int _chunksPerSecond;

    protected Interval _interval;

    protected Applet _applet;

    protected JSObject _doc;

    protected int _failures;

    protected int _maxFailures = 50;
}
