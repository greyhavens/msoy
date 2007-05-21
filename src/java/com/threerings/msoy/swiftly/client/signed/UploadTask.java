//
// $Id$

package com.threerings.msoy.swiftly.client.signed;

import java.awt.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InterruptedIOException;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.swing.ProgressMonitorInputStream;
import javax.swing.UIManager;

import com.threerings.util.MessageBundle;

import com.samskivert.swing.util.TaskAdapter;

import com.threerings.msoy.swiftly.client.ProjectPanel;
import com.threerings.msoy.swiftly.data.ProjectRoomObject;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.util.SwiftlyContext;

public class UploadTask extends TaskAdapter
{
    public static final String SUCCEEDED = "m.file_upload_complete";
    public static final String ABORTED = "m.abort_upload_complete";

    /** Upload block size is 256K to avoid Presents freakouts. */
    public static final int UPLOAD_BLOCK_SIZE = 262144;

    public UploadTask (File file, Component parent, ProjectRoomObject roomObj, SwiftlyContext ctx)
    {
        super();
        _file = file;
        _parent = parent;
        _roomObj = roomObj;
        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(SwiftlyCodes.SWIFTLY_MSGS);
    }

    @Override
    public Object invoke()
        throws Exception
    {
        // tweak the dialog title and cancel button of the standard progress monitor
        UIManager.put("ProgressMonitor.progressText", _msgs.get("m.dialog.upload_progress.title"));
        UIManager.put("OptionPane.cancelButtonText", _msgs.get("m.dialog.upload_progress.cancel"));

        String value = ABORTED;
        try {
            value = AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                public String run() throws Exception {
                    // create a progress monitor to inform the user of the file upload status
                    ProgressMonitorInputStream input = new ProgressMonitorInputStream(_parent,
                        _msgs.get("m.dialog.upload_progress") + _file.getName(),
                        new FileInputStream(_file));
                    int len;
                    byte[] buf = new byte[UPLOAD_BLOCK_SIZE];
                    try {
                        while ((len = input.read(buf)) > 0) {
                            if (len < UPLOAD_BLOCK_SIZE) {
                                byte[] nbuf = new byte[len];
                                System.arraycopy(buf, 0, nbuf, 0, len);
                                _roomObj.service.uploadFile(_ctx.getClient(), nbuf);
                            } else {
                                _roomObj.service.uploadFile(_ctx.getClient(), buf);
                            }
                            // Presents itself does some queueing/sleeping so just keep the reads
                            // happening at roughly the speed the messages are actually being sent
                            // so the feedback the progress monitor is giving reflects some kind
                            // of reality.
                            Thread.sleep(800);
                        }
                    } catch (InterruptedIOException iie) {
                        // user hit cancel, abort the task.
                        return ABORTED;
                    } finally {
                        input.close();
                    }
                    return SUCCEEDED;
                }
            });
        } catch (PrivilegedActionException e) {
            throw e.getException();
        }
        return value;
    }

    protected File _file;
    protected Component _parent;
    protected ProjectRoomObject _roomObj;
    protected SwiftlyContext _ctx;
    protected MessageBundle _msgs;
}
