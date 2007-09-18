//
// $Id$

package com.threerings.msoy.swiftly.client.model;

import com.threerings.msoy.swiftly.client.TranslationMessage;
import com.threerings.msoy.swiftly.client.controller.NewPathElement;
import com.threerings.msoy.swiftly.client.view.PositionLocation;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;
import com.threerings.msoy.swiftly.data.SwiftlyTextDocument;

/**
 * A delegate for making RPC calls on the DocumentModel.
 */
public interface DocumentModelDelegate
{
    public enum FailureCode implements TranslatableError {
        LOAD_DOCUMENT_FAILED (new BundleTranslationMessage("e.load_document_failed")),
        ADD_DOCUMENT_FAILED (new BundleTranslationMessage("e.add_document_failed")),
        UPDATE_DOCUMENT_FAILED (new BundleTranslationMessage("e.update_document_failed")),
        ADD_DIRECTORY_FAILED (new BundleTranslationMessage("e.add_directory_failed")),
        RENAME_ELEMENT_FAILED (new BundleTranslationMessage("e.rename_element_failed")),
        DELETE_ELEMENT_FAILED (new BundleTranslationMessage("e.delete_element_failed"));

        FailureCode (TranslationMessage msg)
        {
            _msg = msg;
        }

        // from TranslatableError
        public TranslationMessage getMessage ()
        {
            return _msg;
        }

        private final TranslationMessage _msg;
    }

    /**
     * Success/failure callbacks for a loadDocument call.
     */
    public void documentLoaded (RequestId requestId, SwiftlyDocument doc,
                                PositionLocation location);
    public void documentLoadingFailed (RequestId requestId, PathElement element,
                                       FailureCode error);

    /**
     * Success/failure callbacks for an addDocument call.
     */
    public void documentAdded (RequestId requestId, NewPathElement newElement);
    public void documentAdditionFailed (RequestId requestId, NewPathElement newElement,
                                        FailureCode error);

    /**
     * Success/failure callbacks for a deleteDocument call.
     */
    public void documentDeleted (RequestId requestId, PathElement element);
    public void documentDeleteFailed (RequestId requestId, PathElement element,
                                      FailureCode error);

    /**
     * Success/failure callbacks for an updateTextDocument call.
     */
    public void textDocumentUpdated (RequestId requestId, SwiftlyTextDocument doc);
    public void textDocumentUpdateFailed (RequestId requestId, SwiftlyTextDocument doc,
                                          FailureCode error);

    /**
     * Success/failure callbacks for a addPathElement call.
     */
    public void directoryAdded (RequestId requestId, PathElement element);
    public void directoryAdditionFailed (RequestId requestId, PathElement element,
                                         FailureCode error);

    /**
     * Success/failure callbacks for a renamePathElement call.
     */
    public void pathElementRenamed (RequestId requestId, PathElement element);
    public void pathElementRenameFailed (RequestId requestId, PathElement element,
                                         FailureCode error);
}
