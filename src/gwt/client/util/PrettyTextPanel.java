//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.DOM;

public class PrettyTextPanel extends Widget 
{
    /**
     * This class parses out the incoming (untrusted) text, and builds up a DOM tree around
     * markup, keeping the untrusted text rendered only as text (not as HTML).
     *
     * This class extends Widget instead of Panel in order to prevent arbitrary additions of
     * other Widgets.
     */
    public PrettyTextPanel (String plainText) 
    {
        setElement(DOM.createDiv());
        pipeline[0].parse(plainText, getElement(), 0);
    }

    protected static void passDownPipe (String plainText, Element parent, int thisStage) {
        if (thisStage + 1 < pipeline.length) {
            pipeline[thisStage + 1].parse(plainText, parent, thisStage + 1);
        } else {
            // end of the pipeline has been reached - create a div for this text
            Element div = DOM.createDiv();
            DOM.setInnerText(div, plainText);
            DOM.setStyleAttribute(div, "display", "inline"); 
            DOM.appendChild(parent, div); 
        }
    }

    protected interface Parser
    {
        /**
         * parse the given plain text, adding the resulting elements to parent, with the 
         * knowledge that we are currently at the given pipeline stage.
         */
        public void parse (String plainText, Element parent, int stage);
    }

    /**
     * This is an array of parsers, wherein each parser needs to accept text, and parse it for
     * what it is interested in, and then pass the parts its not interested in down the pipeline.
     *
     * This action proceeds recursively, in order to make it easy to create HTML structures.  
     */
    protected static final Parser[] pipeline = {
        // paragraph parser
        new Parser() {
            public void parse (String plainText, Element parent, int stage) 
            {
                String[] paragraphs = plainText.split("\n");
                for (int ii = 0; ii < paragraphs.length; ii++) {
                    if (paragraphs[ii].trim().length() > 0) {
                        Element p = DOM.createElement("p");
                        passDownPipe(paragraphs[ii].trim(), p, stage);
                        DOM.appendChild(parent, p);
                    }
                }
            }
        },

        // link parser
        new Parser() {
            public void parse (String plainText, Element parent, int stage)
            {
                passDownPipe(plainText, parent, stage);
            }
        }
    };
}
