//
// $Id$

package com.threerings.msoy.server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.samskivert.util.Interval;
import com.samskivert.util.LoopingThread;
import com.samskivert.util.Queue;
import com.samskivert.util.Throttle;
import com.threerings.msoy.server.MsoyEvents.MsoyEvent;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.common.event.EventDataFactory;
import com.threerings.panopticon.common.net.otp.message.event.EventDataSerializer;

import static com.threerings.msoy.Log.log;

/**
 * Logs Whirled events locally so that we can start collecting event data now rather than when
 * Panopticon is actually operational.
 */
public class LocalEventLogger extends LoopingThread
{
    public static final Integer VERSION_ID = 0xcdefcdef;

    /**
     * Creates an logger that logs to the specified file.
     */
    public LocalEventLogger (File fullpath)
    {
        super("LocalEventLogger");

        _factory = new EventDataFactory();

        _logPath = fullpath;
        openLog(true);

        // update the day format
        _dayStamp = _dayFormat.format(new Date());
        scheduleNextRolloverCheck();
    }

    /**
     * Records the supplied event to the log.
     */
    public void log (MsoyEvent event)
    {
        // append it to our queue, the logging thread will subsequently record it
        _events.append(event);
    }

    @Override // from LoopingThread
    protected void iterate ()
    {
        MsoyEvent event = _events.get();
        if (event == null) {
            return; // we're shutting down
        }

        try {
            // serialize our event into a byte buffer
            ByteArrayOutputStream encoded = new ByteArrayOutputStream();
            EventData eventdata = _factory.getEventData(event);
            EventDataSerializer.serialize(eventdata, encoded);

            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(bout);
            oout.writeObject(Integer.valueOf(VERSION_ID));
            oout.writeObject(encoded.toByteArray());
            oout.close();
            ByteBuffer data = ByteBuffer.wrap(bout.toByteArray());

            // create another buffer that contains the length of the serialized event
            bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);
            dout.writeInt(data.capacity());
            ByteBuffer size = ByteBuffer.wrap(bout.toByteArray());

            // write both to the log file using the scatter/gather interface
            _logChannel.write(new ByteBuffer[] { size, data });

        } catch (Exception e) {
            // be careful about logging zillions of errors if something bad happens to our log file
            if (_throttle.throttleOp()) {
                _throttled++;
            } else {
                if (_throttled > 0) {
                    log.warning("Suppressed " + _throttled + " intervening error messages.");
                    _throttled = 0;
                }
                log.warning("Failed to record event " + event + ".", e);
            }
        }
    }

    @Override // from LoopingThread
    protected void kick ()
    {
        _events.append(null);
    }

    @Override // from LoopingThread
    protected void didShutdown ()
    {
        close();
    }

    protected synchronized void close ()
    {
        if (_logChannel != null) {
            try {
                _logChannel.close();
            } catch (Exception e) {
                log.warning("Failure closing log stream.", e);
            }
            _logChannel = null;
        }
    }

    protected synchronized void openLog (boolean freakout)
    {
        try {
            // create the file channel to which we'll log
            _logChannel = new FileOutputStream(_logPath, true).getChannel();

        } catch (Exception e) {
            String errmsg = "Unable to open audit log '" + _logPath + "'.";
            if (freakout) {
                throw new RuntimeException(errmsg, e);
            } else {
                log.warning(errmsg, e);
            }
        }
    }

    protected synchronized void checkRollOver ()
    {
        // check to see if we should roll over the log
        String newDayStamp = _dayFormat.format(new Date());

        // hey! we need to roll it over!
        if (!newDayStamp.equals(_dayStamp)) {
            // close the old log file
            close();

            // rename the old file
            String npath = _logPath.getPath() + "." + _dayStamp;
            if (!_logPath.renameTo(new File(npath))) {
                log.warning("Failed to rename audit log file [path=" + _logPath +
                            ", npath=" + npath + "].");
            }

            // open our new log file
            openLog(false);

            // and set the next day stamp
            _dayStamp = newDayStamp;
        }

        scheduleNextRolloverCheck();
    }

    protected void scheduleNextRolloverCheck ()
    {
        Calendar cal = Calendar.getInstance();

        // schedule the next check for the next hour mark
        long nextCheck = (1000L - cal.get(Calendar.MILLISECOND)) +
            (59L - cal.get(Calendar.SECOND)) * 1000L +
            (59L - cal.get(Calendar.MINUTE)) * (1000L * 60L);

        _rollover.schedule(nextCheck);
    }

    /** The interval that rolls over the log file. */
    protected Interval _rollover = new Interval() {
        public void expired () {
            checkRollOver();
        }
    };

    /** Converts events to EventData instances. */
    protected EventDataFactory _factory;

    /** The path to our log file. */
    protected File _logPath;

    /** We actually write to this feller here. */
    protected FileChannel _logChannel;

    /** A queue to which events are posted and from which our thread reads and logs them. */
    protected Queue<MsoyEvent> _events = new Queue<MsoyEvent>();

    /** Suppress freakouts if our log file becomes hosed. */
    protected Throttle _throttle = new Throttle(2, 5*60*1000L);

    /** Used to count the number of throttled messages for reporting. */
    protected int _throttled;

    /** The daystamp of the log file we're currently writing to. */
    protected String _dayStamp;

    /** Used to format log file suffixes. */
    protected SimpleDateFormat _dayFormat = new SimpleDateFormat("yyyyMMdd");
}
