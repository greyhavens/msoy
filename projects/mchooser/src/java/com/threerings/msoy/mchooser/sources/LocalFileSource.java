//
// $Id$

package com.threerings.msoy.mchooser.sources;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import java.util.logging.Level;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import com.threerings.msoy.mchooser.Config;
import com.threerings.msoy.mchooser.MediaChooser;
import com.threerings.msoy.mchooser.MediaSource;

import static com.threerings.msoy.mchooser.MediaChooser.log;

/**
 * Allows the user to select files from their local file system.
 */
public class LocalFileSource
    implements MediaSource
{
    public LocalFileSource (String type)
    {
        _type = type;
    }

    // from interface MediaSource
    public String getName ()
    {
        return "Your Computer";
    }

    // from interface MediaSource
    public JComponent createChooser (final ResultReceiver receiver)
    {
        // set up our starting directory
        File startDir = new File(MediaChooser.prefs.get(START_DIR_KEY,
                                                        System.getProperty("user.home")));

        final JFileChooser chooser = new JFileChooser(startDir);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);

        // set up our filters
        if (_type == Config.IMAGE) {
            chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("Image files", "jpg", "jpeg", "gif", "png", "bmp"));
        } else if (_type == Config.AUDIO) {
            chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("Audio files", "mp3"));
        } else if (_type == Config.VIDEO) {
            chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("Video files", "flv", "mpg", "mpeg", "mov", "avi"));
        } else if (_type == Config.FLASH) {
            chooser.addChoosableFileFilter(
                new FileNameExtensionFilter(
                    "Visualizable files", "swf", "jpg", "jpeg", "gif", "png", "bmp"));
        } else if (_type == Config.CODE) {
            chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("Code files", "swf", "jar", "zip"));
        }

        // set up a listener that will report the selection back to the chooser
        chooser.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                if (event.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                    File selection = chooser.getSelectedFile();
                    try {
                        // make a note of our current directory
                        MediaChooser.prefs.put(START_DIR_KEY, selection.getParent());
                        receiver.mediaSelected(selection.toURI().toURL());
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Failed to generate URL for " +
                                chooser.getSelectedFile() + ".", e);
                    }
                }
            }
        });
        return chooser;
    }

    /** We'd use the JDK version but that's 1.6 only and we want to support 1.4. */
    protected static class FileNameExtensionFilter extends FileFilter
    {
        public FileNameExtensionFilter (String descrip, String... extensions) {
            _description = descrip;
            _extensions = extensions;
        }

        @Override // from FileFiler
        public boolean accept (File file) {
            return (file != null && (file.isDirectory() || matches(file.getName())));
        }

        @Override // from FileFiler
        public String getDescription() {
            return _description;
        }

        protected boolean matches (String name)
        {
            String fext = name.substring(name.lastIndexOf(".")+1).toLowerCase();
            for (String ext : _extensions) {
                if (fext.equals(ext)) {
                    return true;
                }
            }
            return false;
        }

        protected String _description;
        protected String[] _extensions;
    }

    protected String _type;

    protected static final String START_DIR_KEY = "localFileStart";
}
