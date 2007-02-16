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
import com.jswiff.swfrecords.LangCode;

import java.io.IOException;


/**
 * <p>
 * Like <code>DefineFontInfo</code>, this tag also maps a glyph font (defined
 * with <code>DefineFont</code>) to a device font, providing a font name,
 * style attributes (e.g. bold, italic) and a table of characters matching the
 * glyph shape table contained in the corresponding <code>DefineFont</code>
 * tag, thereby defining a one-to-one mapping between glyphs and characters.
 * </p>
 * 
 * <p>
 * Unlike <code>DefineFontInfo</code>, <code>DefineFontInfo2</code> contains a
 * field for a language code, making text behavior independent on the locale
 * in which Flash Player is running. This field is considered e.g. when
 * determining line breaking rules. Also, the ANSI and ShiftJIS encodings are
 * not available anymore, as Unicode encoding is used.
 * </p>
 * 
 * <p>
 * Note: Consider using <code>DefineFont2</code> instead of the
 * <code>DefineFont</code> - <code>DefineFontInfo2</code> tag pair, as it
 * incorporates the same functionality in a single tag.
 * </p>
 * 
 * <p>
 * Note: despite its name, this tag isn't a definition tag. It doesn't define a
 * new character, it specifies attributes for an existing character.
 * </p>
 *
 * @see DefineFontInfo
 * @see DefineFont
 * @see DefineFont2
 * @since SWF 6
 */
public final class DefineFontInfo2 extends Tag {
	private int fontId;
	private String fontName;
	private boolean smallText; // glyphs aligned on pixel boundaries
	private boolean italic;
	private boolean bold;
	private LangCode langCode;
	private char[] codeTable;

	/**
	 * Creates a new DefineFontInfo2 tag.
	 *
	 * @param fontId character ID from <code>DefineFont</code>
	 * @param fontName font name, direct (e.g. 'Times New Roman') or indirect
	 * 		  (e.g. '_serif')
	 * @param codeTable table of characters matching the glyph shape table of
	 * 		  the font
	 * @param langCode font language code
	 */
	public DefineFontInfo2(
		int fontId, String fontName, char[] codeTable, LangCode langCode) {
		code			   = TagConstants.DEFINE_FONT_INFO_2;
		this.fontId		   = fontId;
		this.fontName	   = fontName;
		this.codeTable     = codeTable;
		this.langCode	   = langCode;
	}

	DefineFontInfo2() {
		// empty
	}

	/**
	 * Sets/clears bold style.
	 *
	 * @param bold <code>true</code> for bold, otherwise <code>false</code>
	 */
	public void setBold(boolean bold) {
		this.bold = bold;
	}

	/**
	 * Checks if the text is bold.
	 *
	 * @return <code>true</code> if text is bold, otherwise <code>false</code>
	 */
	public boolean isBold() {
		return bold;
	}

	/**
	 * Sets the font's code table containing a character for each glyph.
	 *
	 * @param codeTable code table of font
	 */
	public void setCodeTable(char[] codeTable) {
		this.codeTable = codeTable;
	}

	/**
	 * Returns the font's code table containing a character for each glyph.
	 *
	 * @return code table of font
	 */
	public char[] getCodeTable() {
		return codeTable;
	}

	/**
	 * Sets the character ID of the font this tag specifies attributes for.
	 *
	 * @param fontId character ID of font
	 */
	public void setFontId(int fontId) {
		this.fontId = fontId;
	}

	/**
	 * Returns the character ID of the font.
	 *
	 * @return font's character ID
	 */
	public int getFontId() {
		return fontId;
	}

	/**
	 * Sets the name of the font. This can be either a direct (e.g. 'Times New
	 * Roman') or an indirect font name (e.g. '_serif').
	 *
	 * @param fontName font name as string
	 */
	public void setFontName(String fontName) {
		this.fontName = fontName;
	}

	/**
	 * Returns the name of the font. This can be either a direct (e.g. 'Times
	 * New Roman') or an indirect font name (e.g. '_serif').
	 *
	 * @return font name as string
	 */
	public String getFontName() {
		return fontName;
	}

	/**
	 * Sets/clears italic style.
	 *
	 * @param italic <code>true</code> for italic, otherwise <code>false</code>
	 */
	public void setItalic(boolean italic) {
		this.italic = italic;
	}

	/**
	 * Checks if the text is italic.
	 *
	 * @return <code>true</code> if text is italic, otherwise
	 * 		   <code>false</code>
	 */
	public boolean isItalic() {
		return italic;
	}

	/**
	 * Sets the language code of the font.
	 *
	 * @param langCode font language code
	 */
	public void setLangCode(LangCode langCode) {
		this.langCode = langCode;
	}

	/**
	 * Returns the language code of the font.
	 *
	 * @return font language code
	 */
	public LangCode getLangCode() {
		return langCode;
	}

	/**
	 * Sets the value of the smallFont flag. When this flag is set, the font is
	 * optimized for small text (anti-aliasing is disabled).
	 *
	 * @param smallText <code>true</code> if flag set, else <code>false</code>
	 */
	public void setSmallText(boolean smallText) {
		this.smallText = smallText;
	}

	/**
	 * Checks the smallFont flag. When this flag is set, the font is optimized
	 * for small text (anti-aliasing is disabled).
	 *
	 * @return <code>true</code> if set, else <code>false</code>
	 */
	public boolean isSmallText() {
		return smallText;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		outStream.writeUI16(fontId);
		byte[] fontNameBuffer = (fontName != null) ? fontName.getBytes("UTF-8")
												   : new byte[0];
		outStream.writeUI8((short) (fontNameBuffer.length)); // font name length, not null terminated!
		outStream.writeBytes(fontNameBuffer);
		outStream.writeUnsignedBits(0, 2); // 2 reserved bits
		outStream.writeBooleanBit(smallText);
		outStream.writeUnsignedBits(0, 2); // shiftJIS and ansi not set
		outStream.writeBooleanBit(italic);
		outStream.writeBooleanBit(bold);
		outStream.writeBooleanBit(true); // wideCodes always set
		outStream.writeUI8(langCode.getLanguageCode());
		for (int i = 0; i < codeTable.length; i++) {
			outStream.writeUI16(codeTable[i]);
		}
	}

	void setData(byte[] data) throws IOException {
		InputBitStream inStream = new InputBitStream(data);
		fontId = inStream.readUI16();
		short fontNameLen = inStream.readUI8(); // not null-terminated!
		fontName = new String(inStream.readBytes(fontNameLen), "UTF-8");
		inStream.readUnsignedBits(2); // 2 reserved bits
		smallText = inStream.readBooleanBit();
		inStream.readBooleanBit(); // shiftJIS, always 0
		inStream.readBooleanBit(); // ANSI, always 0
		italic		  = inStream.readBooleanBit();
		bold		  = inStream.readBooleanBit();
		// dataStream.align(); // wideCodes always true - but align not needed before readUI8()
		langCode	  = new LangCode((byte) inStream.readUI8());
		int codeTableSize = (int) ((data.length - inStream.getOffset()) / 2);
		codeTable     = new char[codeTableSize];
		for (int i = 0; i < codeTableSize; i++) {
			codeTable[i] = (char) inStream.readUI16();
		}
	}
}
