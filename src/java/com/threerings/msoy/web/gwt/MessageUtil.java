//
// $Id$

package com.threerings.msoy.web.gwt;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.HTML;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MediaDesc;

/**
 * Message (forum and other) related utilities.
 */
public class MessageUtil
{
    /**
     * Expands various special formatting codes (and data) contained in a supplied HTML message.
     * Currently supports _BOX_ and _URL_.
     */
    public static String expandMessage (String html)
    {
        // parse our segments from the raw HTML
        List<Segment> segs = parseSegments(html);

        // group box segments together
        List<Box> group = null;
        for (int ii = 0; ii < segs.size(); ii++) {
            Segment seg = segs.get(ii);
            if (!(seg instanceof Box)) { // we don't group non-box segments
                group = null; // end our accumulating group
                continue;
            }
            Box box = (Box)seg;
            if (group != null && group.get(0).size == box.size) {
                group.add(box);
                segs.remove(ii--);
            } else {
                group = new ArrayList<Box>();
                group.add(box);
                segs.set(ii, new Group(group));
            }
        }

        // format the segments
        StringBuffer buf = new StringBuffer();
        for (Segment seg : segs) {
            seg.format(buf);
        }

        return buf.toString();
    }

    /**
     * Converts formatting commands into <div> tags that will render sanely in the forum message
     * editor.
     */
    public static String preEditMessage (String html)
    {
        List<Segment> segs = parseSegments(html);

        // format the segments
        StringBuffer buf = new StringBuffer();
        for (Segment seg : segs) {
            seg.preEdit(buf);
        }

        return buf.toString();
    }

    public static String makeBox (String token, MediaDesc desc, int size, String name)
    {
        return BOX_START + token + "\t" + MediaDesc.mdToString(desc) + "\t" + size + "\t" + name +
            BOX_END;
    }

    protected static List<Segment> parseSegments (String html)
    {
        List<Segment> segs = new ArrayList<Segment>();
        segs.add(new Text(html));
        int size;
        do {
            size = segs.size();
            // we reread segs.size() every time through the loop as it expands
            for (int ii = 0; ii < segs.size(); ii++) {
                if (segs.get(ii) instanceof Text) {
                    expandBox(segs, ii);
                }
            }
        } while (size != segs.size());
        return segs;
    }

    protected static void expandBox (List<Segment> segs, int index)
    {
        Text text = (Text)segs.get(index);
        String html = text.text;

        int sidx = html.indexOf(BOX_START);
        if (sidx == -1) {
            return;
        }
        int eidx = html.indexOf(BOX_END, sidx);
        if (eidx == -1) {
            return;
        }

        segs.remove(index);
        String pre = html.substring(0, sidx).trim();
        if (pre.length() > 0) {
            segs.add(index++, new Text(pre));
        }
        segs.add(index++, new Box(html.substring(sidx + BOX_START.length(), eidx)));
        String post = html.substring(eidx + BOX_END.length()).trim();
        if (post.length() > 0) {
            segs.add(index++, new Text(post));
        }
    }

    protected static String expandURL (String html)
    {
        return null;
    }

    protected static String escapeHTML (String text)
    {
        return text.replaceAll("&", "&amp;")
                   .replaceAll("<", "&lt;")
                   .replaceAll(">", "&gt;")
                   .replaceAll("\"", "&quot;");
    }

    protected static abstract class Segment {
        public abstract void format (StringBuffer buf);

        public void preEdit (StringBuffer buf) {
            format(buf);
        }
    }

    protected static class Text extends Segment {
        public final String text;

        public Text (String text) {
            this.text = text;
        }

        public void format (StringBuffer buf) {
            buf.append(text);
        }
    }

    protected static class Box extends Segment {
        public final String token;
        public final MediaDesc desc;
        public final int size;
        public final String name;

        public Box (String text) {
            String[] bits = text.split("\t", 4);
            this.token = bits[0];
            this.desc = MediaDesc.stringToMD(bits[1]);
            int msize;
            try {
                msize = Integer.parseInt(bits[2]);
            } catch (NumberFormatException e) {
                msize = MediaDesc.THUMBNAIL_SIZE;
            }
            this.size = msize;
            this.name = bits[3];
        }

        public void format (StringBuffer buf) {
//             int width = MediaDesc.getWidth(size);
//             int height = MediaDesc.getHeight(size);
            buf.append("<div class='thingBox'><a href='");
            buf.append(DeploymentConfig.serverURL).append("#").append(token).append("'>");
            if (desc != null) {
                buf.append("<img src='").append(desc.getMediaPath()).append("'>");
            }
            buf.append("<div class='Name'>").append(escapeHTML(name)).append("</div>");
            buf.append("</a></div>\n");
        }

        public void preEdit (StringBuffer buf) {
            buf.append(DeploymentConfig.serverURL).append("#").append(token).append("<br/>");
        }
    }

    protected static class Group extends Segment {
        public final List<Box> boxes;

        public Group (List<Box> boxes) {
            this.boxes = boxes;
        }

        public void format (StringBuffer buf) {
            int cols;
            switch (boxes.get(0).size) {
            case MediaDesc.THUMBNAIL_SIZE: cols = 4; break;
            case MediaDesc.GAME_SHOT_SIZE: cols = 2; break;
            default:
            case MediaDesc.SNAPSHOT_FULL_SIZE: cols = 1; break;
            }

            int col = 0;
            buf.append("<table width='100%' cellspacing='0' cellpadding='5'>");
            for (Box box : boxes) {
                if (col == 0) {
                    buf.append("<tr>");
                }
                buf.append("<td align='center'>\n");
                box.format(buf);
                buf.append("</td>");
                if (col == cols-1) {
                    buf.append("</tr>");
                }
                col = (col + 1) % cols;
            }
            if (col != 0) {
                buf.append("</tr>");
            }
            buf.append("</table>\n");
        }

        public void preEdit (StringBuffer buf) {
            throw new RuntimeException("Not implemented.");
        }
    }

    protected static final String BOX_START = "<box>";
    protected static final String BOX_END = "</box>";
}
