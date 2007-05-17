//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.applet.AppletContext;
import java.applet.AppletStub;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.net.URL;

import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.swing.JFrame;

import static com.threerings.msoy.Log.log;

/**
 * Allows Swiftly to be run from the command line. Expected arguments:
 * username= password= projectId= server= port=
 */
public class SwiftlyStandAlone
{
    static public void main (String argv[]) {
        final SwiftlyApplet applet = new SwiftlyApplet();
        JFrame frame = new JFrame("Swiftly Applet Test Window");
        frame.addWindowListener (new WindowAdapter()
        {
            public void windowClosing (WindowEvent event)
            {
                applet.stop();
                applet.destroy();
                System.exit(0);
            }
        });
        frame.setContentPane(applet);
        // TODO: need to setup an applet context too
        applet.setStub(new SwiftlyAppletStub(argv, applet));
        applet.init();
        applet.start();
        // TODO: allow this to be set on the command line?
        frame.setSize(1024, 768);
        frame.pack();
        frame.setVisible(true);
    }
    
    protected static class SwiftlyAppletStub
        implements AppletStub
    {
        SwiftlyAppletStub (String argv[], SwiftlyApplet applet)
        {
            _applet = applet;
            _properties = new HashMap<String, String>();

            for (int i = 0; i < argv.length; i++) {
                try {
                    StringTokenizer parser = new StringTokenizer(argv[i], "=");
                    String name = parser.nextToken().toString();
                    String value = parser.nextToken("\"").toString();
                    value = value.substring(1);
                    _properties.put(name, value);
                } catch (NoSuchElementException e) {
                    e.printStackTrace();
                }
            }

            // TODO: sanity check _properties for missing values
        }
    
        // from AppletStub
        public void appletResize (int width, int height)
        {
            _applet.resize(width, height);
        }

        // from AppletStub
        public AppletContext getAppletContext ()
        {
            // TODO:
            return null;
        }

        // from AppletStub
        public URL getCodeBase()
        {
            // TODO:
            return null;
        }

        // from AppletStub
        public URL getDocumentBase ()
        {
            // TODO:
            return null;
        }

        // from AppletStub
        public String getParameter (String name)
        {
            return _properties.get(name);
        }

        // from AppletStub
        public boolean isActive ()
        {
            return true;
        }
        
        SwiftlyApplet _applet;
        HashMap<String, String> _properties;
    }
}
