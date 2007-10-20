//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.applet.AudioClip;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Image;

import java.io.File;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.SwingConstants;

import com.google.common.collect.Maps;
import com.samskivert.swing.MultiLineLabel;

/**
 * Allows Swiftly to be run from the command line. Expected arguments:
 * username= password= projectId= server= port=
 */
public class SwiftlyStandAlone
{
    static public void main (String argv[])
    {
        // parse and check the arguments
        Map<String, String> properties = parseArguments(argv);

        final SwiftlyApplet applet = new SwiftlyApplet();
        JFrame frame = new JFrame("Swiftly Applet Test Window");
        frame.addWindowListener (new WindowAdapter()
        {
            @Override
            public void windowClosing (WindowEvent event)
            {
                applet.stop();
                applet.destroy();
                System.exit(0);
            }
        });
        frame.setContentPane(applet);
        applet.setStub(new SwiftlyAppletStub(applet, properties));
        // TODO: allow this to be set on the command line?
        frame.setSize(1024, 768);
        frame.setVisible(true);
        applet.init();
        applet.start();
    }

    private static Map<String, String> parseArguments (String argv[])
    {
        Map<String, String> properties = Maps.newHashMap();
        for (int i = 0; i < argv.length; i++) {
            try {
                StringTokenizer parser = new StringTokenizer(argv[i], "=");
                String name = parser.nextToken().toString();
                String value = parser.nextToken("\"").toString();
                value = value.substring(1);
                properties.put(name, value);
            } catch (NoSuchElementException e) {
                e.printStackTrace();
            }
        }

        // check the command line arguments for missing values
        String errorMsg = "";
        if (!(properties.containsKey("username") || properties.containsKey("password"))) {
            errorMsg = errorMsg + "Username and password required. Use username= password=\n";
        }
        if (!(properties.containsKey("server") || properties.containsKey("port"))) {
            errorMsg = errorMsg + "Server and port required. Use server= port=\n";
        }
        if (!properties.containsKey("projectId")) {
            errorMsg = errorMsg + "projectId required. Use projectId=\n";
        }

        // report any errors and quit
        if (!errorMsg.equals("")) {
            System.err.println("Missing arguments:");
            System.err.println(errorMsg);
            System.exit(1);
        }

        return properties;
    }

    private static class SwiftlyAppletStub
        implements AppletStub
    {
        SwiftlyAppletStub (SwiftlyApplet applet, Map<String, String> properties)
        {
            _applet = applet;
            _properties = properties;
            _context = new SwiftlyAppletContext();
        }

        public void appletResize (int width, int height)
        {
            _applet.resize(width, height);
        }

        public AppletContext getAppletContext ()
        {
            return _context;
        }

        public URL getCodeBase ()
        {
            try {
                return new File(System.getProperty("user.dir")).toURI().toURL();
            } catch (MalformedURLException e) {
                System.err.println("Unable to determine the code base URL");
                return null;
            }
        }

        public URL getDocumentBase ()
        {
            return getCodeBase();
        }

        public String getParameter (String name)
        {
            return _properties.get(name);
        }

        public boolean isActive ()
        {
            return true;
        }

        SwiftlyApplet _applet;
        Map<String, String> _properties;
        AppletContext _context;
    }

    private static class SwiftlyAppletContext
        implements AppletContext
    {
        public Applet getApplet (String name)
        {
            throw new NotImplementedException();
        }

        public Enumeration<Applet> getApplets ()
        {
            throw new NotImplementedException();
        }

        public AudioClip getAudioClip (URL url)
        {
            throw new NotImplementedException();
        }

        public Image getImage (URL url)
        {
            throw new NotImplementedException();
        }

        public InputStream getStream (String key)
        {
            throw new NotImplementedException();
        }

        public Iterator<String> getStreamKeys ()
        {
            throw new NotImplementedException();
        }

        public void setStream (String key, InputStream stream)
        {
            throw new NotImplementedException();
        }

        public void showDocument (URL url)
        {
            showDocument(url, "_blank");
        }

        public void showDocument (URL url, String target)
        {
            JFrame frame = new JFrame("showDocument() window");
            frame.setContentPane(new MultiLineLabel(
                "showDocument() called wth [url=" + url + " target=" + target + "].",
                SwingConstants.CENTER, SwingConstants.HORIZONTAL, 400));
            frame.pack();
            frame.setVisible(true);
        }

        public void showStatus (String status)
        {
            System.out.println("showStatus() called with: " + status);
        }

        private static class NotImplementedException extends RuntimeException
        {
            NotImplementedException ()
            {
                super("Not implemented in AppletContext");
            }
        }
    }

}
