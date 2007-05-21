//
// $Id$

package com.threerings.msoy.swiftly.client.signed;

import java.awt.Component;

import java.io.File;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.swing.JFileChooser;

public class SignedFileChooser
{
    public SignedFileChooser ()
    {
        // because the constructor performs secure operations we cannot subclass JFileChooser
        // as we cannot wrap super() in AccessController.doPrivileged
        _fc = AccessController.doPrivileged(new PrivilegedAction<JFileChooser>() {
            public JFileChooser run() {
                return new JFileChooser();
            }
        });
    }

    public int showOpenDialog (final Component parent)
    {
        Integer value = AccessController.doPrivileged(new PrivilegedAction<Integer>() {
            public Integer run() {
                return new Integer(_fc.showOpenDialog(parent));
            }
        });
        return value.intValue();
    }

    public long getSelectedFileLength ()
    {
        Long value = AccessController.doPrivileged(new PrivilegedAction<Long>() {
            public Long run() {
                return new Long(_fc.getSelectedFile().length());
            }
        });
        return value.longValue();
    }

    public String getSelectedFileName ()
    {
        String value = AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return _fc.getSelectedFile().getName();
            }
        });
        return value;
    }

    public File getSelectedFile ()
    {
        return _fc.getSelectedFile();
    }

    public void setApproveButtonText (String text)
    {
        _fc.setApproveButtonText(text);
    }

    protected JFileChooser _fc;
    protected String _approveButtonText;
}
