//
// $Id$

package com.threerings.msoy.swiftly.client.model;

import com.threerings.msoy.swiftly.client.TranslationMessage;
import com.threerings.msoy.swiftly.data.BuildResult;

/**
 * A delegate for making RPC calls on the ProjectModel.
 */
public interface ProjectModelDelegate
{
    public enum FailureCode implements TranslatableError {
        BUILD_REQUEST_FAILED (new BundleTranslationMessage("e.build_failed_unexpected")),
        BUILD_EXPORT_REQUEST_FAILED (
            new BundleTranslationMessage("e.build_export_failed_unexpected"));

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
     * Success/failure callbacks for a buildProject call.
     */
    public void buildRequestSucceeded (RequestId requestId, BuildResult result);
    public void buildRequestFailed (RequestId requestId, FailureCode error);

    /**
     * Success/failure callbacks for a buildAndExportProject call.
     */
    public void buildAndExportRequestSucceeded (RequestId requestId, BuildResult result);
    public void buildAndExportRequestFailed (RequestId requestId, FailureCode error);
}
