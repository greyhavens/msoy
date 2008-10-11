//
// $Id$

package client.editem;

/**
 * Configures the UI nicely for our bulk media types (photos, audio, video).
 */
public abstract class BulkMediaEditor extends ItemEditor
{
    @Override // from ItemEditor
    protected void addInfo ()
    {
        super.addInfo();
        addSpacer();
        addTab(_emsgs.editorTabMain());
        addSpacer();
        addMainUploader();
        addTab(_emsgs.editorTabExtras());
    }

    /**
     * Override this and add your primary media uploader.
     */
    protected abstract void addMainUploader ();

    @Override // from ItemEditor
    protected void addDescription ()
    {
        switchToTab(0);
        super.addDescription();
    }
}
