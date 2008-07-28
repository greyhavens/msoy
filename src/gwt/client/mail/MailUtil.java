//
// $Id$

package client.mail;

/**
 * Mail related utility methods.
 */
public class MailUtil
{
    /**
     * Scans a text, generating HTML that respects leading/consecutive spaces and newlines, while
     * escaping actual HTML constructs.
     */
    public static String textToHTML (String message)
    {
        StringBuffer html = new StringBuffer();
        boolean collectSpaces = true;
        for (int i = 0; i < message.length(); i ++) {
            String bit;
            char c = message.charAt(i);
            switch(c) {
            case '\r':
                // completely ignore
                continue;
            case '<':
                // escape HTML
                bit = "&lt;";
                collectSpaces = false;
                break;
            case '>':
                // escape HTML
                bit = "&gt;";
                collectSpaces = false;
                break;
            case '&':
                // escape HTML
                bit = "&amp;";
                collectSpaces = false;
                break;
            case '\n':
                // a newline is replaced by a HTML break
                bit = "<br>\n";
                collectSpaces = true;
                break;
            case ' ': case '\t':
                // a single space is left alone, unless it leads a line
                if (!collectSpaces) {
                    collectSpaces = true;
                    bit = null;
                    break;
                }
                // but a leading space or consecutive spaces are replaced by these
                bit = "&nbsp;";
                break;
            default:
                collectSpaces = false;
                bit = null;
                break;
            }
            if (bit != null) {
                html.append(bit);
            } else {
                html.append(c);
            }
        }
        return html.toString();
    }
}
