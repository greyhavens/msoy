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

package com.jswiff.swfrecords;

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;

import java.io.IOException;
import java.io.Serializable;


/**
 * A TextRecord contains a group of characters which share the same style and
 * are on the same text line. It is used within <code>DefineText</code> and
 * <code>DefineText2</code> tags.
 *
 * @see com.jswiff.swfrecords.tags.DefineText
 * @see com.jswiff.swfrecords.tags.DefineText2
 */
public final class TextRecord implements Serializable {
  private boolean hasXOffset;
  private short xOffset;
  private boolean hasYOffset;
  private short yOffset;
  private boolean hasFont;
  private int fontId;
  private int textHeight;
  private boolean hasColor;
  private Color textColor;
  private GlyphEntry[] glyphEntries;

  /**
   * Creates a new TextRecord instance. The text contained in this instance is
   * defined by a list of references to entries from the text font's glyph
   * table.
   *
   * @param glyphEntries glyph entries (indexes in glyph table)
   */
  public TextRecord(GlyphEntry[] glyphEntries) {
    this.glyphEntries = glyphEntries;
  }

  /**
   * Reads a new TextRecord instance from a bit stream.
   *
   * @param stream source stream
   * @param glyphBits bit count used for glyph index representation
   * @param advanceBits bit count used for advance value representation
   * @param hasAlpha specifies whether transparency is supported or not
   *
   * @throws IOException if an I/O error has occured
   */
  public TextRecord(
    InputBitStream stream, short glyphBits, short advanceBits, boolean hasAlpha)
    throws IOException {
    stream.readUnsignedBits(4); // ignore first 4 bits
    hasFont        = stream.readBooleanBit();
    hasColor       = stream.readBooleanBit();
    hasYOffset     = stream.readBooleanBit();
    hasXOffset     = stream.readBooleanBit();
    if (hasFont) {
      fontId = stream.readUI16();
    }
    if (hasColor) {
      textColor = (hasAlpha) ? ((Color) new RGBA(stream))
                             : ((Color) new RGB(stream));
    }
    if (hasXOffset) {
      xOffset = stream.readSI16();
    }
    if (hasYOffset) {
      yOffset = stream.readSI16();
    }
    if (hasFont) {
      textHeight = stream.readUI16();
    }
    int glyphCount = stream.readUI8();
    glyphEntries = new GlyphEntry[glyphCount];
    for (int i = 0; i < glyphCount; i++) {
      glyphEntries[i] = new GlyphEntry(stream, glyphBits, advanceBits);
    }
    stream.align();
  }

  /**
   * Sets the font of the text. If omitted, the preceding text record's font is
   * used.
   *
   * @param fontId character ID of the font
   * @param textHeight font height in twips (1/20 px)
   */
  public void setFont(int fontId, int textHeight) {
    this.fontId       = fontId;
    this.textHeight   = textHeight;
    hasFont           = true;
  }

  /**
   * Returns the character ID of the font used. Check with
   * <code>hasFont()</code> first if the font has been specified. The font can
   * be omitted when the font doesn't change between succeeding text records
   * (e.g. after a line break).
   *
   * @return character ID of used font
   */
  public int getFontId() {
    return fontId;
  }

  /**
   * Returns the glyph entries, i.e. a list of references to entries from the
   * used font's glyph table. The text characters contained in a text record
   * are defined by this list.
   *
   * @return glyph entries (indexes in glyph table)
   */
  public GlyphEntry[] getGlyphEntries() {
    return glyphEntries;
  }

  /**
   * Sets the color of the text. Can be an <code>RGB</code> (if used within
   * <code>DefineText</code>) or an <code>RGBA</code> instance (if used within
   * <code>DefineText2</code>).
   *
   * @param textColor text color
   */
  public void setTextColor(Color textColor) {
    this.textColor   = textColor;
    hasColor         = true;
  }

  /**
   * Returns the color of the text. Can be an <code>RGB</code> (if used within
   * <code>DefineText</code>) or an <code>RGBA</code> instance (if used within
   * <code>DefineText2</code>).
   *
   * @return color of text
   */
  public Color getTextColor() {
    return textColor;
  }

  /**
   * Returns the text height (the font size in twips). Check with
   * <code>hasFont()</code> first if the font and its size has been specified.
   * The font can be omitted when the font doesn't change between succeeding
   * text records (e.g. after a line break).
   *
   * @return font size in twips (1/20 px)
   */
  public int getTextHeight() {
    return textHeight;
  }

  /**
   * Sets the horizontal offset from the left of the text bounds (specified in
   * <code>DefineText</code> / <code>DefineText2</code>) to the reference
   * point of the first glyph (in twips). Used for indented or
   * non-left-justified text.
   *
   * @param offset x offset of text in twips (1/20 px)
   */
  public void setXOffset(short offset) {
    xOffset      = offset;
    hasXOffset   = true;
  }

  /**
   * Returns the horizontal offset from the left of the text bounds (specified
   * in <code>DefineText</code> / <code>DefineText2</code>) to the reference
   * point of the first glyph (in twips). Used for indented or
   * non-left-justified text.
   *
   * @return horizontal offset
   */
  public short getXOffset() {
    return xOffset;
  }

  /**
   * Sets the vertical offset from the top of the text bounds (specified in
   * <code>DefineText</code> / <code>DefineText2</code>) to the reference
   * point of the first glyph (in twips). Used e.g. for line breaks.
   *
   * @param offset y offset of text in twips (1/20 px)
   */
  public void setYOffset(short offset) {
    yOffset      = offset;
    hasYOffset   = true;
  }

  /**
   * Returns the vertical offset from the top of the text bounds (specified in
   * <code>DefineText</code> / <code>DefineText2</code>) to the reference
   * point of the first glyph (in twips). Used e.g. for line breaks.
   *
   * @return vertical offset
   */
  public short getYOffset() {
    return yOffset;
  }

  /**
   * Checks if the text color has been specified.
   *
   * @return <code>true</code> if text color specified, else <code>false</code>
   */
  public boolean hasColor() {
    return hasColor;
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
   * Checks if an x offset has been specified.
   *
   * @return <code>true</code> if x offset specified, else <code>false</code>
   */
  public boolean hasXOffset() {
    return hasXOffset;
  }

  /**
   * Checks if an y offset has been specified.
   *
   * @return <code>true</code> if y offset specified, else <code>false</code>
   */
  public boolean hasYOffset() {
    return hasYOffset;
  }

  /**
   * Writes text record to a bit stream.
   *
   * @param stream target bit stream
   * @param glyphBits bit count used for glyph index representation
   * @param advanceBits bit count used for advance value representation
   *
   * @throws IOException if an I/O error has occured
   */
  public void write(OutputBitStream stream, short glyphBits, short advanceBits)
    throws IOException {
    stream.writeUnsignedBits(1, 1);
    stream.writeUnsignedBits(0, 3);
    stream.writeBooleanBit(hasFont);
    stream.writeBooleanBit(hasColor);
    stream.writeBooleanBit(hasYOffset);
    stream.writeBooleanBit(hasXOffset);
    if (hasFont) {
      stream.writeUI16(fontId);
    }
    if (hasColor) {
      textColor.write(stream);
    }
    if (hasXOffset) {
      stream.writeSI16(xOffset);
    }
    if (hasYOffset) {
      stream.writeSI16(yOffset);
    }
    if (hasFont) {
      stream.writeUI16(textHeight);
    }
    stream.writeUI8((short) glyphEntries.length);
    for (int i = 0; i < glyphEntries.length; i++) {
      glyphEntries[i].write(stream, glyphBits, advanceBits);
    }
    stream.align();
  }
}
