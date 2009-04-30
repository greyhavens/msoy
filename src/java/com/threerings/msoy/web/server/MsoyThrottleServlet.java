//
// $Id$

package com.threerings.msoy.web.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Used only for testing, this fakey slows-down the transmission of data to clients.
 * It can be configured with -Dthrottle=true or -DthrottleMedia=true.
 */
public class MsoyThrottleServlet extends MsoyDefaultServlet
{
    @Override
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws ServletException, IOException
    {
        // if we're only throttling media, see if this is media
        if (Boolean.getBoolean("throttle") || req.getRequestURI().startsWith("/media/")) {
            rsp = new HttpServletResponseWrapper(rsp) {
                @Override
                public ServletOutputStream getOutputStream () throws IOException {
                    if (_out == null) {
                        _out = new ThrottleOutputStream(super.getOutputStream());
                    }
                    return _out;
                }
                // We explicitly mirror our parent class' deprecation of these methods to
                // prevent the compiler from complaining.
                @Deprecated
                public String encodeRedirectUrl (String arg0) {
                    return super.encodeRedirectUrl(arg0);
                }
                @Deprecated
                public String encodeUrl (String arg0) {
                    return super.encodeUrl(arg0);
                }
                @Deprecated
                public void setStatus (int arg0, String arg1) {
                    super.setStatus(arg0, arg1);
                }
                protected ServletOutputStream _out;
            };
        }
        super.doGet(req, rsp);
    }

    /**
     * Caaaaan youuuuuu heeeeaaaaaaaaarrrrrrrrrr mmmmmeeeee??
     */
    protected static class ThrottleOutputStream extends ServletOutputStream
    {
        public ThrottleOutputStream (ServletOutputStream out) {
            _out = out;
        }

        public void write (int i) throws IOException {
            _out.write(i);
        }

        public void write (byte[] b, int off, int len) throws IOException {
            while (len > 0) {
                int toWrite = Math.min(len, 1024);
                _out.write(b, off, toWrite);
                off += toWrite;
                len -= toWrite;
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ie) {
                }
            }
        }

        public void print (boolean arg) throws IOException {
            _out.print(arg);
        }
        public void print (char arg) throws IOException {
            _out.print(arg);
        }
        public void print (double arg) throws IOException {
            _out.print(arg);
        }
        public void print (float arg) throws IOException {
            _out.print(arg);
        }
        public void print (int arg) throws IOException {
            _out.print(arg);
        }
        public void print (long arg) throws IOException {
            _out.print(arg);
        }
        public void print (String arg) throws IOException {
            _out.print(arg);
        }
        public void println () throws IOException {
            _out.println();
        }
        public void println (boolean arg) throws IOException {
            _out.println(arg);
        }
        public void println (char arg) throws IOException {
            _out.println(arg);
        }
        public void println (double arg) throws IOException {
            _out.println(arg);
        }
        public void println (float arg) throws IOException {
            _out.println(arg);
        }
        public void println (int arg) throws IOException {
            _out.println(arg);
        }
        public void println (long arg) throws IOException {
            _out.println(arg);
        }
        public void println (String arg) throws IOException {
            _out.println(arg);
        }

        protected ServletOutputStream _out;
    }
}
