//
// $Id: $

package com.threerings.msoy.util;

import java.io.IOException;
import com.threerings.io.BasicStreamers;
import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.BasicStreamers.BasicStreamer;

import static com.threerings.msoy.Log.log;

/**
 * Brutally replace the normal Narya streamer for strings with one that encodes and decodes
 * real UTF-8 rather than Java's modification to the format. We do this in Whirled so that the
 * ActionScript layer can use its natural string encoding.
 *
 * Note: THIS IS A HORRIFIC HACK that makes angels weep.
 *
 * Note: Any custom readObject/writeObject methods must take care to call readUnmodifiedUTF
 * and writeUnmodifiedUTF directly.
 */
public class UnmodifiedUTFStringStreamer
{
    public static void inject ()
    {
        for (int ii = 0; ii < BasicStreamers.BSTREAMER_TYPES.length; ii++) {
            if (BasicStreamers.BSTREAMER_TYPES[ii].equals(String.class)) {
                BasicStreamers.BSTREAMER_INSTANCES[ii] = new Streamer();
                log.info("UTF8 Streamer hack inserted!");
                return;
            }
        }
        throw new IllegalStateException("Could not find String streamer to replace");
    }

    protected static final class Streamer extends BasicStreamer
    {
        public Object createObject (ObjectInputStream in)
            throws IOException
        {
            return in.readUnmodifiedUTF();
        }

        public void writeObject (Object object, ObjectOutputStream out, boolean useWriter)
            throws IOException
        {
            out.writeUnmodifiedUTF((String) object);
        }
    }
}
