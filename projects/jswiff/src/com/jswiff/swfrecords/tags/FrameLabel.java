/*
 * JSwiff is an open source Java API for Macromedia Flash file generation
 * and manipulation
 *
 * Copyright (C) 2004-2005 Ralf Terdic (contact@jswiff.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.jswiff.swfrecords.tags;

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;

import java.io.IOException;


/**
 * <p>
 * This tag assigns a certain name to the current frame. This name can be used
 * by <code>GoToLabel</code> to identify this frame.
 * </p>
 * 
 * <p>
 * As of SWF 6, labels can additionally be defined as named anchors for better
 * browser integration. Named anchors are similar to HTML anchors (i.e.
 * fragment identifiers, as specified in RFC 2396). If the named anchor is
 * supplied at the end of the SWF file's URL (like
 * <code>http://servername/filename.swf#named_anchor</code>) in the browser,
 * the Flash Player plugin starts playback at the frame labeled as
 * <code>named_anchor</code>. Additionally, if the Flash Player plugin
 * encounters a frame containing a named anchor during playback of an SWF, it
 * adds the anchor to the URL of the HTML page embedding the SWF in the
 * address bar (or updates it if an anchor is already there), so the frame can
 * be bookmarked and the browser's "back" and "forward" buttons can be used
 * for navigation.
 * </p>
 *
 * @since SWF 3 (named anchors since SWF 6)
 */
public final class FrameLabel extends Tag {
	private String name;
	private boolean isNamedAnchor;

	/**
	 * Creates a new FrameLabel tag.
	 *
	 * @param name label name
	 * @param isNamedAnchor set to <code>true</code> if label is named anchor,
	 * 		  otherwise <code>false</code>
	 */
	public FrameLabel(String name, boolean isNamedAnchor) {
		code				   = TagConstants.FRAME_LABEL;
		this.name			   = name;
		this.isNamedAnchor     = isNamedAnchor;
	}

	FrameLabel() {
		// empty
	}

	/**
	 * Sets the name of the label assigned to the frame.
	 *
	 * @param name label name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the name of the label assigned to the frame.
	 *
	 * @return label name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Specifies whether the label is defined as named anchor.
	 *
	 * @param isNamedAnchor <code>true</code> if label is named anchor,
	 * 		  otherwise <code>false</code>
	 */
	public void setNamedAnchor(boolean isNamedAnchor) {
		this.isNamedAnchor = isNamedAnchor;
	}

	/**
	 * Checks whether the label is defined as named anchor.
	 *
	 * @return <code>true</code> if label is named anchor, otherwise
	 * 		   <code>false</code>
	 */
	public boolean isNamedAnchor() {
		return isNamedAnchor;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		forceLongHeader = true;
		outStream.writeString(name);
		if ((getSWFVersion() >= 6) && isNamedAnchor) {
			outStream.writeUI8((short) 1);
		}
	}

	void setData(byte[] data) throws IOException {
		InputBitStream inStream = new InputBitStream(data);
    if (getSWFVersion() < 6) {
      if (isJapanese()) {
        inStream.setShiftJIS(true);
      } else {
        inStream.setANSI(true);
      }
    }
		name = inStream.readString();
		if ((getSWFVersion() >= 6) && (inStream.available() > 0)) {
			isNamedAnchor = (inStream.readUI8() != 0);
		}
	}
}
