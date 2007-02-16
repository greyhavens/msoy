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
import com.jswiff.swfrecords.RGBA;
import com.jswiff.swfrecords.Rect;

import java.io.IOException;


/**
 * <p>
 * This tag defines a dynamic text field. A text field can be associated with a
 * variable the contents of the text field are stored in and kept in sync
 * with.
 * </p>
 * 
 * <p>
 * Users may change the value of a text field interactively, unless the
 * <code>readOnly</code> tag is set.
 * </p>
 * 
 * <p>
 * Fonts used by this tag must be defined using <code>DefineFont2</code> (not
 * <code>DefineFont</code>). If the <code>useOutlines</code> flag is cleared, the
 * Flash Player will try to render text using device fonts rather than glyph
 * fonts.
 * </p>
 * 
 * <p>
 * When the <code>html</code> flag is set, the text may contain HTML tags.
 * Allowed tags are:
 * 
 * <ul>
 * <li>
 * <code>&lt;p&gt;...&lt;/p&gt;</code>: paragraph
 * </li>
 * <li>
 * <code>&lt;br&gt;</code>: line break
 * </li>
 * <li>
 * <code>&lt;a href="..."&gt;...&lt;/a&gt;</code>: hyperlink. Optional
 * attributes:
 * 
 * <ul>
 * <li>
 * <code>target</code>: a window name
 * </li>
 * </ul>
 * 
 * </li>
 * <li>
 * <code>&lt;font&gt;...&lt;/font&gt;</code>: font properties. Available
 * attributes:
 * 
 * <ul>
 * <li>
 * <code>face</code>: font name supplied in a <code>DefineFont2</code> tag.
 * </li>
 * <li>
 * <code>size</code> in twips (1/20 px). Leading <code>+</code> or
 * <code>-</code> indicates a relative size
 * </li>
 * <li>
 * <code>color</code> as a <code>#RRGGBB</code> hex value
 * </li>
 * </ul>
 * 
 * </li>
 * <li>
 * <code>&lt;b&gt;...&lt;/b&gt;</code>: bold text
 * </li>
 * <li>
 * <code>&lt;i&gt;...&lt;/i&gt;</code>: italic text
 * </li>
 * <li>
 * <code>&lt;u&gt;...&lt;/u&gt;</code>: underlined text
 * </li>
 * <li>
 * <code>&lt;li&gt;...&lt;/li&gt;</code>: bulleted list. Warning: &lt;ul&gt; is
 * not supported
 * </li>
 * <li>
 * <code>&lt;textformat&gt;...&lt;/textformat&gt;</code> lets you specify
 * formatting attributes:
 * 
 * <ul>
 * <li>
 * <code>leftmargin</code> in twips (1/20 px)
 * </li>
 * <li>
 * <code>rightmargin</code> in twips
 * </li>
 * <li>
 * <code>indent</code> in twips
 * </li>
 * <li>
 * <code>blockindent</code> in twips
 * </li>
 * <li>
 * <code>leading</code> in twips
 * </li>
 * <li>
 * <code>tabstops</code>: comma-separated list in twips
 * </li>
 * </ul>
 * 
 * </li>
 * <li>
 * <code>&lt;tab&gt;</code>: advances to next tab stop (defined with
 * <code>&lt;textformat&gt;</code>)
 * </li>
 * </ul>
 * </p>
 * 
 * <p>
 * Multiple other parameters can be set, e.g. text layout attributes like
 * margins and leading etc.
 * </p>
 *
 * @since SWF 4
 */
public final class DefineEditText extends DefinitionTag {
	/** Left text alignment */
	public static final short ALIGN_LEFT    = 0;
	/** Right text alignment */
	public static final short ALIGN_RIGHT   = 1;
	/** Center text alignment */
	public static final short ALIGN_CENTER  = 2;
	/** Justify text alignment */
	public static final short ALIGN_JUSTIFY = 3;
	private Rect bounds;
	private boolean wordWrap;
	private boolean multiline;
	private boolean password;
	private boolean readOnly;
	private boolean autoSize;
	private boolean noSelect;
	private boolean border;
	private boolean html;
	private boolean useOutlines;
	private int fontId					    = -1;
	private int fontHeight;
	private RGBA textColor;
	private int maxLength;
	private short align;
	private int leftMargin;
	private int rightMargin;
	private int indent;
	private int leading;
	private String variableName;
	private String initialText;
	private boolean hasText;
	private boolean hasTextColor;
	private boolean hasMaxLength;
	private boolean hasFont;
	private boolean hasLayout;

	/**
	 * Creates a new DefineEditText tag. Specify the character ID of the text
	 * field, its bounds and the name of the variable the contents of the text
	 * field are stored in and kept in sync with.
	 *
	 * @param characterId character ID of the text field
	 * @param bounds the size of the rectangle which completely encloses the
	 * 		  text field.
	 * @param variableName variable name (in dot or slash syntax)
	 */
	public DefineEditText(int characterId, Rect bounds, String variableName) {
		code				  = TagConstants.DEFINE_EDIT_TEXT;
		this.characterId	  = characterId;
		this.bounds			  = bounds;
		this.variableName     = variableName;
	}

	DefineEditText() {
		// empty
	}

	/**
	 * Returns the text alignment (one of the constants <code>ALIGN_LEFT,
	 * ALIGN_RIGHT, ALIGN_CENTER, ALIGN_JUSTIFY</code>). Check with
	 * <code>hasLayout()</code> first if value is set.
	 *
	 * @return text alignment
	 */
	public short getAlign() {
		return align;
	}

	/**
	 * Sets the value of the <code>autoSize</code> flag. If set, the text field
	 * resizes depending on its content size.
	 *
	 * @param autoSize <code>true</code> if flag set, else <code>false</code>
	 */
	public void setAutoSize(boolean autoSize) {
		this.autoSize = autoSize;
	}

	/**
	 * Checks if the <code>autoSize</code> flag is set, i.e. if the text field
	 * is supposed to resize depending on its content size.
	 *
	 * @return <code>true</code> if flag set, else <code>false</code>
	 */
	public boolean isAutoSize() {
		return autoSize;
	}

	/**
	 * Sets the value of the <code>border</code> flag. A set flag causes a
	 * border to be drawn around the text field.
	 *
	 * @param border <code>true</code> if flag set, else <code>false</code>
	 */
	public void setBorder(boolean border) {
		this.border = border;
	}

	/**
	 * Checks if the <code>border</code> flag, causing a border to be drawn
	 * around the text field.
	 *
	 * @return <code>true</code> if <code>border</code> flag is set, else
	 * 		   <code>false</code>
	 */
	public boolean isBorder() {
		return border;
	}

	/**
	 * Returns the bounds of the text field, i.e. the size of the rectangle
	 * which completely encloses the text field.
	 *
	 * @return text field bounds
	 */
	public Rect getBounds() {
		return bounds;
	}

	/**
	 * Sets the font of the text.
	 *
	 * @param fontId character ID of the font
	 * @param fontHeight font height in twips (1/20 px)
	 */
	public void setFont(int fontId, int fontHeight) {
		this.fontId		    = fontId;
		this.fontHeight     = fontHeight;
		hasFont			    = true;
	}

	/**
	 * Returns the font height (in twips, i.e. 1/20 px). Check with
	 * <code>hasFont()</code> first if value is set.
	 *
	 * @return font height in twips
	 */
	public int getFontHeight() {
		return fontHeight;
	}

	/**
	 * Returns the character ID of the used font. Check with
	 * <code>hasFont()</code> first if value is set.
	 *
	 * @return font character ID
	 */
	public int getFontId() {
		return fontId;
	}

	/**
	 * Sets the value of the <code>html</code> flag. If set, html tags are
	 * allowed in the contained text.
	 *
	 * @param html <code>true</code> if flag set, else <code>false</code>
	 */
	public void setHtml(boolean html) {
		this.html = html;
	}

	/**
	 * Returns the <code>html</code> flag, which specifies whether html tags
	 * are allowed in the contained text.
	 *
	 * @return <code>true</code> if <code>html</code> flag is set, otherwise
	 * 		   <code>false</code>
	 */
	public boolean isHtml() {
		return html;
	}

	/**
	 * Returns the text indent in twips (i.e. 1/20 px). Check with
	 * <code>hasLayout()</code> first if value is set.
	 *
	 * @return text indent in twips
	 */
	public int getIndent() {
		return indent;
	}

	/**
	 * Sets the text initially contained in the text field.
	 *
	 * @param initialText initialText
	 */
	public void setInitialText(String initialText) {
		this.initialText     = initialText;
		hasText				 = true;
	}

	/**
	 * Returns the initial text string.  Check with <code>hasText()</code>
	 * first if value is set.
	 *
	 * @return initial text
	 */
	public String getInitialText() {
		return initialText;
	}

	/**
	 * Sets the layout properties of the text: alignment, margins, indentation
	 * and line spacing.
	 *
	 * @param align text alignment, one of the constants <code>ALIGN_LEFT,
	 * 		  ALIGN_RIGHT, ALIGN_CENTER, ALIGN_JUSTIFY</code>
	 * @param leftMargin left text margin in twips (i.e. 1/20 px)
	 * @param rightMargin right text margin
	 * @param indent text indentation in twips
	 * @param leading line spacing in twips
	 */
	public void setLayout(
		short align, int leftMargin, int rightMargin, int indent, int leading) {
		this.align			 = align;
		this.leftMargin		 = leftMargin;
		this.rightMargin     = rightMargin;
		this.indent			 = indent;
		this.leading		 = leading;
		hasLayout			 = true;
	}

	/**
	 * Returns the text leading (line spacing) in twips (i.e. 1/20 px). Check
	 * with <code>hasLayout()</code> first if value is set.
	 *
	 * @return line spacing in twips
	 */
	public int getLeading() {
		return leading;
	}

	/**
	 * Returns the left text margin in twips (i.e. 1/20 px). Check with
	 * <code>hasLayout()</code> first if value is set.
	 *
	 * @return left margin in twips
	 */
	public int getLeftMargin() {
		return leftMargin;
	}

	/**
	 * Restricts the text length to the specified amount of characters.
	 *
	 * @param maxLength maximum text length
	 */
	public void setMaxLength(int maxLength) {
		this.maxLength     = maxLength;
		hasMaxLength	   = true;
	}

	/**
	 * Returns the maximum length of the text. Check with
	 * <code>hasMaxLength()</code> if value is set.
	 *
	 * @return maximum text length
	 */
	public int getMaxLength() {
		return maxLength;
	}

	/**
	 * Sets the value of the <code>multiLine</code> flag. If set, the text
	 * field may contain more than one text line.
	 *
	 * @param multiline <code>true</code> if flag set, else <code>false</code>
	 */
	public void setMultiline(boolean multiline) {
		this.multiline = multiline;
	}

	/**
	 * Checks if the <code>multiLine</code> flag is set. If set, the text field
	 * may contain more than one text line.
	 *
	 * @return <code>true</code> if <code>multiLine</code> flag set, otherwise
	 * 		   <code>false</code>
	 */
	public boolean isMultiline() {
		return multiline;
	}

	/**
	 * Sets the value of the <code>noSelect</code> flag. If set, the text
	 * cannot be interactively selected (and copied).
	 *
	 * @param noSelect <code>true</code> if flag set, else <code>false</code>
	 */
	public void setNoSelect(boolean noSelect) {
		this.noSelect = noSelect;
	}

	/**
	 * Checks if the <code>noSelect</code> flag is set. If set, the text cannot
	 * be interactively selected (and copied).
	 *
	 * @return <code>true</code> if <code>noSelect</code> flag set, otherwise
	 * 		   <code>false</code>
	 */
	public boolean isNoSelect() {
		return noSelect;
	}

	/**
	 * Sets the value of the <code>password</code> flag. If set, the characters
	 * of the text are displayed as asterisks.
	 *
	 * @param password <code>true</code> if flag set, else <code>false</code>
	 */
	public void setPassword(boolean password) {
		this.password = password;
	}

	/**
	 * Checks whether the <code>password</code> flag is set. If set, the
	 * characters of the text are displayed as asterisks.
	 *
	 * @return <code>true</code> if <code>password</code> flag set, otherwise
	 * 		   <code>false</code>
	 */
	public boolean isPassword() {
		return password;
	}

	/**
	 * Specifies the value of the <code>readOnly</code> flag. If set, the text
	 * cannot be interactively edited.
	 *
	 * @param readOnly <code>true</code> if flag set, else <code>false</code>
	 */
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	/**
	 * Checks whether the <code>readOnly</code> flag is set. If set, the text
	 * cannot be interactively edited.
	 *
	 * @return <code>true</code> if <code>readOnly</code> flag set, otherwise
	 * 		   <code>false</code>
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * Returns the right text margin in twips (i.e. 1/20 px). Check with
	 * <code>hasLayout()</code> first if value is set.
	 *
	 * @return right margin in twips
	 */
	public int getRightMargin() {
		return rightMargin;
	}

	/**
	 * Sets the text color to the specified color. Alpha channel (transparency)
	 * information can also be specified.
	 *
	 * @param textColor text color as RGBA value
	 */
	public void setTextColor(RGBA textColor) {
		this.textColor     = textColor;
		hasTextColor	   = true;
	}

	/**
	 * Returns the text color and transparency value.
	 *
	 * @return text color as RGBA value
	 */
	public RGBA getTextColor() {
		return textColor;
	}

	/**
	 * Sets the value of the <code>useOutlines</code> flag. If set, the Flash
	 * Player tries to use device fonts rather than glyph fonts.
	 *
	 * @param useOutlines <code>true</code> if flag set, else
	 * 		  <code>false</code>
	 */
	public void setUseOutlines(boolean useOutlines) {
		this.useOutlines = useOutlines;
	}

	/**
	 * Checks if the <code>useOutlines</code> flag is set. If set, the Flash
	 * Player tries to use device fonts rather than glyph fonts.
	 *
	 * @return <code>true</code> if <code>useOutlines</code> flag set,
	 * 		   otherwise <code>false</code>
	 */
	public boolean isUseOutlines() {
		return useOutlines;
	}

	/**
	 * Sets the name of the variable the contents of the text field are stored
	 * in and kept in sync with.
	 *
	 * @param variableName variable name (in dot or slash syntax)
	 */
	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	/**
	 * Returns the name of the variable the contents of the text field are
	 * stored in and kept in sync with.
	 *
	 * @return variable name (in dot or slash syntax)
	 */
	public String getVariableName() {
		return variableName;
	}

	/**
	 * Sets the value of the <code>wordWrap</code> flag. If set, the text will
	 * wrap at the end of the line.
	 *
	 * @param wordWrap <code>true</code> if flag set, else <code>false</code>
	 */
	public void setWordWrap(boolean wordWrap) {
		this.wordWrap = wordWrap;
	}

	/**
	 * Checks if the <code>wordWrap</code> flag is set. If set, the text will
	 * wrap at the end of the line.
	 *
	 * @return <code>true</code> if <code>wordWrap</code> flag set, otherwise
	 * 		   <code>false</code>
	 */
	public boolean isWordWrap() {
		return wordWrap;
	}

	/**
	 * Checks whether the text font (ID and size) has been specified.
	 *
	 * @return <code>true</code> if font specified, else <code>false</code>
	 */
	public boolean hasFont() {
		return hasFont;
	}

	/**
	 * Checks if the text layout has been specified, i.e. if the following
	 * attributes have been set:
	 * 
	 * <ul>
	 * <li>
	 * align
	 * </li>
	 * <li>
	 * left margin
	 * </li>
	 * <li>
	 * right margin
	 * </li>
	 * <li>
	 * indent
	 * </li>
	 * <li>
	 * leading
	 * </li>
	 * </ul>
	 * 
	 *
	 * @return <code>true</code> if at least one layout attribute set
	 */
	public boolean hasLayout() {
		return hasLayout;
	}

	/**
	 * Checks if the length of the text has been restricted to a maximum value.
	 *
	 * @return <code>true</code> if maximum text length is set, else
	 * 		   <code>false</code>
	 */
	public boolean hasMaxLength() {
		return hasMaxLength;
	}

	/**
	 * Checks if an initial text is provided.
	 *
	 * @return <code>true</code> if initial text set, else <code>false</code>
	 */
	public boolean hasText() {
		return hasText;
	}

	/**
	 * Checks if the text color is set.
	 *
	 * @return <code>true</code> if text color set, else <code>false</code>
	 */
	public boolean hasTextColor() {
		return hasTextColor;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		outStream.writeUI16(characterId);
		bounds.write(outStream);
		outStream.writeBooleanBit(hasText);
		outStream.writeBooleanBit(wordWrap);
		outStream.writeBooleanBit(multiline);
		outStream.writeBooleanBit(password);
		outStream.writeBooleanBit(readOnly);
		outStream.writeBooleanBit(hasTextColor);
		outStream.writeBooleanBit(hasMaxLength);
		outStream.writeBooleanBit(hasFont);
		outStream.writeUnsignedBits(0, 1); // 1 reserved bit
		outStream.writeBooleanBit(autoSize);
		outStream.writeBooleanBit(hasLayout);
		outStream.writeBooleanBit(noSelect);
		outStream.writeBooleanBit(border);
		outStream.writeUnsignedBits(0, 1); // 1 reserved bit
		outStream.writeBooleanBit(html);
		outStream.writeBooleanBit(useOutlines);
		if (hasFont) {
			outStream.writeUI16(fontId);
			outStream.writeUI16(fontHeight);
		}
		if (hasTextColor) {
			textColor.write(outStream);
		}
		if (hasMaxLength) {
			outStream.writeUI16(maxLength);
		}
		if (hasLayout) {
			outStream.writeUI8(align);
			outStream.writeUI16(leftMargin);
			outStream.writeUI16(rightMargin);
			outStream.writeUI16(indent);
			outStream.writeUI16(leading);
		}
		if (variableName != null) {
			outStream.writeString(variableName);
		} else {
			outStream.writeString("");
		}
		if (hasText) {
			outStream.writeString(initialText);
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
		characterId		 = inStream.readUI16();
		bounds			 = new Rect(inStream);
		hasText			 = inStream.readBooleanBit();
		wordWrap		 = inStream.readBooleanBit();
		multiline		 = inStream.readBooleanBit();
		password		 = inStream.readBooleanBit();
		readOnly		 = inStream.readBooleanBit();
		hasTextColor     = inStream.readBooleanBit();
		hasMaxLength     = inStream.readBooleanBit();
		hasFont			 = inStream.readBooleanBit();
		inStream.readBooleanBit(); // ignore reserved bit
		autoSize	  = inStream.readBooleanBit();
		hasLayout     = inStream.readBooleanBit();
		noSelect	  = inStream.readBooleanBit();
		border		  = inStream.readBooleanBit();
		inStream.readBooleanBit(); // ignore reserved bit
		html			 = inStream.readBooleanBit();
		useOutlines		 = inStream.readBooleanBit();
		if (hasFont) {
			fontId		   = inStream.readUI16();
			fontHeight     = inStream.readUI16();
		}
		if (hasTextColor) {
			textColor = new RGBA(inStream);
		}
		if (hasMaxLength) {
			maxLength = inStream.readUI16();
		}
		if (hasLayout) {
			align		    = inStream.readUI8();
			leftMargin	    = inStream.readUI16();
			rightMargin     = inStream.readUI16();
			indent		    = inStream.readUI16();
			leading		    = inStream.readUI16();
		}
		variableName = inStream.readString();
		if (hasText) {
			initialText = inStream.readString();
		}
	}
}
