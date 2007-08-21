//
// $Id$
package com.threerings.msoy.swiftly.server;

import static com.threerings.msoy.Log.log;

import java.util.logging.Level;

import com.samskivert.util.ResultListener;
import com.samskivert.util.SerialExecutor;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;
import com.threerings.msoy.swiftly.server.storage.ProjectStorage;
import com.threerings.msoy.web.server.UploadFile;

/** Handles inserting the upload file data into svn and the room object. */
public class InsertFileUploadTask
    implements SerialExecutor.ExecutorTask
{
    public InsertFileUploadTask (ProjectRoomManager manager, UploadFile uploadFile,
                                 ResultListener<Void> listener)
    {
        this(manager, uploadFile, null, null, listener);
    }

    public InsertFileUploadTask (ProjectRoomManager manager, UploadFile uploadFile,
                                 PathElement element, SwiftlyDocument doc,
                                 ResultListener<Void> listener)
    {
        _manager = manager;
        // snapshot the root element while we are on the dobject thread
        _rootElement = manager.getRoomObj().getRootElement();

        _uploadFile = uploadFile;
        _element = element;
        _doc = doc;
        _listener = listener;
    }

    public boolean merge (SerialExecutor.ExecutorTask other)
    {
        return false;
    }

    public long getTimeout ()
    {
        return 60 * 1000L; // 60 seconds is all you get kid
    }

    // this is called on the executor thread and can go hog wild with the blocking
    public void executeTask ()
    {
        try {
            // if we were given a valid path element, but the document was not resolved in
            // the room object, we need to load it from the storage repository
            if (_element != null && _doc == null) {
                _doc = _manager.getStorage().getDocument(_element);

            // else if we have no path element, then this is a new document. the final "else"
            // is that we have an existing path element and we already found the document
            // so just proceed.
            } else if (_element == null) {
                // TODO: as we currently have no way of getting from GWT which "directory"
                // the user wants this element to go into, we are going to assume that the root
                // is the parent of the new element.
                _element = PathElement.createFile(_uploadFile.getOriginalName(), _rootElement,
                    _uploadFile.getMimeTypeAsString());

                // create the new, blank SwiftlyDocument
                _doc = SwiftlyDocument.createFromPathElement(_element,
                    ProjectStorage.TEXT_ENCODING);
            }

           // insert the uploaded file data into the new document
           _doc.setData(_uploadFile.getInputStream(), ProjectStorage.TEXT_ENCODING);

           // save the document to the repository
           _manager.getStorage().putDocument(_doc, "Automatic Swiftly Upload Commit");
           _doc.commit();

        } catch (Exception error) {
            // we'll report this on resultReceived()
            _error = error;
        }
    }

    // meanwhile, back on the dobject thread
    public void resultReceived ()
    {
        if (_error != null) {
            log.log(Level.WARNING, "insertUploadFile failed [element=" + _element + "].",
                _error);
            _listener.requestFailed(_error);
            return;
        }

        // publish the element and document in the room object
        _manager.getRoomObj().publishPathElement(_element);
        _manager.getRoomObj().publishSwiftlyDocument(_doc);

        // inform the listener everything worked
        _listener.requestCompleted(null);
    }

    // this is called back on the dobj thread and must only report failure
    public void timedOut ()
    {
        _listener.requestFailed(new Exception("InsertFileUploadTask timed out."));
    }

    protected final ProjectRoomManager _manager;
    protected final UploadFile _uploadFile;
    protected PathElement _element;
    protected SwiftlyDocument _doc;
    protected final PathElement _rootElement;
    protected final ResultListener<Void> _listener;
    protected Exception _error;
}