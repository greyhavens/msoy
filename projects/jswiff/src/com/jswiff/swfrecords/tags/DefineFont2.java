/*
 * JSwiff is an open source Java API for Macromedia Flash file generation
 * and manipulation
 *
 * Copyright (C) 2004-2006 Ralf Terdic (contact@jswiff.com)
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
import com.jswiff.swfrecords.KerningRecord;
import com.jswiff.swfrecords.LangCode;
import com.jswiff.swfrecords.Rect;
import com.jswiff.swfrecords.Shape;

import java.io.IOException;


/**
 * This tag is used to supply font information. Unlike with
 * <code>DefineFont</code>, fonts defined with this tag can be used for
 * dynamic text. Font metrics for improved layout can be supplied. Mapping to
 * device fonts is also possible.
 *
 * @since SWF 3.
 */
public class DefineFont2 extends DefinitionTag {
  private boolean shiftJIS;
  private boolean smallText;
  private boolean ansi;
  private boolean italic;
  private boolean bold;
  private LangCode languageCode;
  private String fontName;
  private Shape[] glyphShapeTable;
  private char[] codeTable;
  private short ascent;
  private short descent;
  private short leading;
  private short[] advanceTable;
  private Rect[] boundsTable;
  private KerningRecord[] kerningTable;
  private int numGlyphs;
  private boolean hasLayout;

  /**
   * <p>
   * Creates a new DefineFont2 tag. Requires the font's character ID and name,
   * a glyph shape table and a code table.
   * </p>
   * 
   * <p>
   * The shape table contains one shape for each glyph. When using dynamic
   * device text, the shape table can be empty (set to <code>null</code>). In
   * this case, the code table is ignored.
   * </p>
   * 
   * <p>
   * The code table is an array of characters equal in size to the shape table.
   * It assigns a character to each glyph.
   * </p>
   *
   * @param characterId character ID of the font
   * @param fontName font name, either direct, e.g. 'Times New Roman', or
   *        indirect, like '_serif'
   * @param glyphShapeTable array of shapes (for each glyph one)
   * @param codeTable array of chars (for each glyph one)
   *
   * @throws IllegalArgumentException if code table is different from glyph
   *         count
   */
  public DefineFont2(
    int characterId, String fontName, Shape[] glyphShapeTable, char[] codeTable) {
    code               = TagConstants.DEFINE_FONT_2;
    this.characterId   = characterId;
    this.fontName      = fontName;
    if (glyphShapeTable != null) {
      this.glyphShapeTable   = glyphShapeTable;
      numGlyphs              = glyphShapeTable.length;
      if (codeTable.length != numGlyphs) {
        throw new IllegalArgumentException(
          "Size of codeTable must be equal to glyph count!");
      }
      this.codeTable = codeTable;
    }
  }

  DefineFont2() {
    // empty
  }

  /**
   * Sets the value of the ANSI flag. If ANSI is set, the shiftJIS flag is
   * cleared. If neither ANSI nor shiftJIS are set, UCS-2 is used.
   *
   * @param ansi <code>true</code> if flag set, else <code>false</code>
   */
  public void setANSI(boolean ansi) {
    this.ansi = ansi;
    if (ansi) {
      shiftJIS = false;
    }
  }

  /**
   * Checks if the ANSI flag is set. If neither ANSI nor shiftJIS are set,
   * UCS-2 is used.
   *
   * @return <code>true</code> if flag set, else <code>false</code>
   */
  public boolean isANSI() {
    return ansi;
  }

  /**
   * Returns an array containing the advance value for each glyph of the font.
   *
   * @return advance table
   */
  public short[] getAdvanceTable() {
    return advanceTable;
  }

  /**
   * Returns the font's ascent. The ascent is the distance from the baseline to
   * the ascender line (i.e. to the highest ascender of the font).
   *
   * @return font ascent (in EM square coords)
   */
  public short getAscent() {
    return ascent;
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
   * Returns the font's bounds table.
   *
   * @return bounds table.
   */
  public Rect[] getBoundsTable() {
    return boundsTable;
  }

  /**
   * Sets the font's code table containing a character for each glyph.
   *
   * @param codeTable font's code table
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
   * Returns the font's descent. The descent is the space below the baseline
   * required by the lowest descender (distance between base line and
   * descender line).
   *
   * @return font descent (in EM square coords)
   */
  public short getDescent() {
    return descent;
  }

  /**
   * Sets the name of the font. This can be either a direct (e.g. 'Times New
   * Roman') or an indirect font name (e.g. '_serif').
   *
   * @param fontName string containing the font's name
   */
  public void setFontName(String fontName) {
    this.fontName = fontName;
  }

  /**
   * Returns the name of the font. This can be either a direct (e.g. 'Times New
   * Roman') or an indirect font name (e.g. '_serif').
   *
   * @return font name as string
   */
  public String getFontName() {
    return fontName;
  }

  /**
   * Sets the glyph shape table, containing a shape for each defined glyph.
   *
   * @param glyphShapeTable glyph shapes array
   */
  public void setGlyphShapeTable(Shape[] glyphShapeTable) {
    this.glyphShapeTable = glyphShapeTable;
  }

  /**
   * Returns the glyph shape table, containing a shape for each defined glyph.
   *
   * @return glyph shapes array
   */
  public Shape[] getGlyphShapeTable() {
    return glyphShapeTable;
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
   * @return <code>true</code> if text is italic, otherwise <code>false</code>
   */
  public boolean isItalic() {
    return italic;
  }

  /**
   * Returns the font's kerning table.
   *
   * @return kerning table
   */
  public KerningRecord[] getKerningTable() {
    return kerningTable;
  }

  /**
   * Sets the language code of this font.
   *
   * @param languageCode a language code
   */
  public void setLanguageCode(LangCode languageCode) {
    this.languageCode = languageCode;
  }

  /**
   * Returns the font's language code.
   *
   * @return a language code
   */
  public LangCode getLanguageCode() {
    return languageCode;
  }

  /**
   * <p>
   * Sets layout information for dynamic glyph text.
   * </p>
   * 
   * <p>
   * The <code>ascent</code> is the distance from the baseline to the ascender
   * line (i.e. to the highest ascender of the font).
   * </p>
   * 
   * <p>
   * The <code>descent</code> is the space below the baseline required by the
   * lowest descender.
   * </p>
   * 
   * <p>
   * The <code>leading</code> is the spacing between descender and ascender
   * line of two successive lines (vertical line spacing).
   * </p>
   * 
   * <p>
   * This method allows the specification of a bounds table and a kerning
   * table. Supply arrays equal in size to the glyph count (which is
   * determined by the size of the glyph shape table), or use
   * <code>null</code> to simplify matters.
   * </p>
   *
   * @param ascent font ascender height (in EM square coords)
   * @param descent font descender height (in EM square coords)
   * @param leading vertical spacing between lines (from descender to next
   *        line's ascender, in EM square coords)
   * @param advanceTable advance values (for each glyph one, in EM square
   *        coords)
   * @param boundsTable glyph bounds table
   * @param kerningTable kerning table
   *
   * @throws IllegalArgumentException if size of boundsTable or kerningTable is
   *         not equal to glyph count
   */
  public void setLayout(
    short ascent, short descent, short leading, short[] advanceTable,
    Rect[] boundsTable, KerningRecord[] kerningTable) {
    hasLayout           = true;
    this.ascent         = ascent;
    this.descent        = descent;
    this.leading        = leading;
    this.advanceTable   = advanceTable;
    if ((advanceTable == null) || (advanceTable.length != numGlyphs)) {
      throw new IllegalArgumentException(
        "Size of advanceTable must be equal to glyph count!");
    }
    if ((boundsTable != null) && (boundsTable.length != numGlyphs)) {
      throw new IllegalArgumentException(
        "Size of boundsTable must be equal to glyph count!");
    }
    this.boundsTable    = boundsTable;
    this.kerningTable   = kerningTable;
  }

  /**
   * Returns the font leading, i.e. the vertical spacing  between the bottom of
   * the descender of one line and the top of the next line's ascender.
   *
   * @return font leading (in EM square coords)
   */
  public short getLeading() {
    return leading;
  }

  /**
   * Sets the value of the shiftJIS flag. If shiftJIS is set, then ANSI is
   * cleared. If neither ANSI nor shiftJIS are set, UCS-2 is used.
   *
   * @param shiftJIS <code>true</code> if flag set, else <code>false</code>
   */
  public void setShiftJIS(boolean shiftJIS) {
    this.shiftJIS = shiftJIS;
    if (shiftJIS) {
      ansi = false;
    }
  }

  /**
   * Checks if the shiftJIS flag is set. If neither ANSI nor shiftJIS are set,
   * UCS-2 is used.
   *
   * @return <code>true</code> if flag set, else <code>false</code>
   */
  public boolean isShiftJIS() {
    return shiftJIS;
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

  /**
   * Checks if layout information is supplied (ascent, descent, leading,
   * advance).
   *
   * @return <code>true</code> if layout info supplied, else <code>false</code>
   */
  public boolean hasLayout() {
    return hasLayout;
  }

  protected void writeData(OutputBitStream outStream) throws IOException {
    outStream.writeUI16(characterId);
    outStream.writeBooleanBit(hasLayout);
    outStream.writeBooleanBit(shiftJIS);
    outStream.writeBooleanBit(smallText);
    outStream.writeBooleanBit(ansi);
    // compute offsetTable (offsets start at 0
    long[] offsetTable               = new long[numGlyphs];

    // implicit: offsetTable[0] = 0;
    // later on, we apply correction to the offsets:
    // we add the offsetTable size and the size of codeTableOffset (2 or 4) to each offset
    //
    // use a byte array bit stream for writing shapeTable to compute the offsets
    OutputBitStream shapeTableStream = new OutputBitStream();
    if (numGlyphs > 0) {
      // the first offset is known (0); write first shapeTable entry
      glyphShapeTable[0].write(shapeTableStream);
    }
    for (int i = 1; i < numGlyphs; i++) {
      // store offset of shape to offset table
      offsetTable[i] = shapeTableStream.getOffset();
      // write shape
      glyphShapeTable[i].write(shapeTableStream);
    }
    long codeTableOffset = shapeTableStream.getOffset();

    // last offset is the biggest - do we need 32 bits, or just 16?
    // apply correction (offsetTable and codeTableOffset) and compare to 2^16-1
    boolean wideOffsets  = false; // we don't need wide offsets if tables empty 
    if (numGlyphs > 0) {
      wideOffsets = ((offsetTable[numGlyphs - 1] + (2 * (numGlyphs + 1))) > 65535);
    }

    // add offsetTable size and codeTableOffset size to each offset
    long offsetCorrection = (numGlyphs + 1) * (wideOffsets ? 4 : 2);
    for (int i = 0; i < numGlyphs; i++) {
      offsetTable[i] += offsetCorrection;
    }
    codeTableOffset += offsetCorrection;
    outStream.writeBooleanBit(wideOffsets);
    // in SWF6 and later, unicode is used, so use wideCodes=true as default
    // only if ansi or shiftJIS is set, set wideCodes=false
    boolean wideCodes = (!(ansi || shiftJIS));
    outStream.writeBooleanBit(wideCodes);
    outStream.writeBooleanBit(italic);
    outStream.writeBooleanBit(bold);
    if (languageCode != null) {
      outStream.writeUI8(languageCode.getLanguageCode());
    } else {
      outStream.writeUI8((short) 0);
    }
    byte[] fontNameBuffer = (fontName != null) ? fontName.getBytes("UTF-8")
                                               : new byte[0];

    // null-terminated or not... Flash 2004 terminates with 0, so we do this too
    outStream.writeUI8((short) (fontNameBuffer.length + 1));
    outStream.writeBytes(fontNameBuffer);
    outStream.writeUI8((short) 0);
    outStream.writeUI16(numGlyphs);
    // write offsetTable and codeTableOffset
    if (wideOffsets) {
      for (int i = 0; i < numGlyphs; i++) {
        outStream.writeUI32((offsetTable[i]));
      }
      outStream.writeUI32(codeTableOffset);
    } else {
      for (int i = 0; i < numGlyphs; i++) {
        outStream.writeUI16((int) offsetTable[i]);
      }
      outStream.writeUI16((int) codeTableOffset);
    }

    // write glyphShapeTable (from bit stream data)
    byte[] shapeTableBuffer = shapeTableStream.getData();
    if (shapeTableBuffer.length > 0) {
      outStream.writeBytes(shapeTableBuffer);
    }

    // write codeTable, depending on wideCodes UI16 or UI8
    if (wideCodes) {
      for (int i = 0; i < numGlyphs; i++) {
        outStream.writeUI16(codeTable[i]);
      }
    } else {
      for (int i = 0; i < numGlyphs; i++) {
        outStream.writeUI8((short) codeTable[i]);
      }
    }
    if (hasLayout) {
      outStream.writeSI16(ascent);
      outStream.writeSI16(descent);
      outStream.writeSI16(leading);
      for (int i = 0; i < numGlyphs; i++) {
        outStream.writeSI16(advanceTable[i]);
      }

      // bounds table
      if (boundsTable == null) {
        for (int i = 0; i < numGlyphs; i++) {
          (new Rect(0, 0, 0, 0)).write(outStream);
        }
      } else {
        for (int i = 0; i < numGlyphs; i++) {
          boundsTable[i].write(outStream);
        }
      }

      // kerning
      if (kerningTable == null) {
        outStream.writeUI16(0); // kerningCount = 0
      } else {
        outStream.writeUI16(kerningTable.length);
        for (int i = 0; i < kerningTable.length; i++) {
          kerningTable[i].write(outStream, wideCodes);
        }
      }
    }
  }

  void setData(byte[] data) throws IOException {
    InputBitStream inStream = new InputBitStream(data);
    characterId    = inStream.readUI16();
    hasLayout      = inStream.readBooleanBit();
    shiftJIS       = inStream.readBooleanBit();
    smallText      = inStream.readBooleanBit();
    ansi           = inStream.readBooleanBit();
    boolean wideOffsets = inStream.readBooleanBit();
    boolean wideCodes = inStream.readBooleanBit();
    italic         = inStream.readBooleanBit();
    bold           = inStream.readBooleanBit();
    languageCode   = new LangCode((byte) inStream.readUI8());
    short fontNameLen = inStream.readUI8();
    byte[] fontNameBuffer = inStream.readBytes(fontNameLen);
    if ((fontNameLen > 0) && (fontNameBuffer[fontNameLen - 1] == 0)) {
      fontNameLen--;
    }
    fontName    = new String(fontNameBuffer, 0, fontNameLen, "UTF-8");
    numGlyphs   = inStream.readUI16();
    // skip offsets, we don't need them
    if (wideOffsets) {
      // skip offsetTable, UI32
      inStream.readBytes(numGlyphs * 4);
      // skip codeTableOffset, UI32
      inStream.readBytes(4);
    } else {
      // skip offsetTable, UI16
      inStream.readBytes(numGlyphs * 2);
      // skip CodeTableOffset, UI16
      inStream.readBytes(2);
    }
    if (numGlyphs > 0) {
      glyphShapeTable = new Shape[numGlyphs];
      for (int i = 0; i < numGlyphs; i++) {
        glyphShapeTable[i] = new Shape(inStream);
      }
      codeTable = new char[numGlyphs];
      if (wideCodes) {
        for (int i = 0; i < numGlyphs; i++) {
          codeTable[i] = (char) inStream.readUI16();
        }
      } else {
        for (int i = 0; i < numGlyphs; i++) {
          codeTable[i] = (char) inStream.readUI8();
        }
      }
    }
    if (hasLayout) {
      ascent    = inStream.readSI16();
      descent   = inStream.readSI16();
      leading   = inStream.readSI16();
      if (numGlyphs > 0) {
        advanceTable = new short[numGlyphs];
        for (int i = 0; i < numGlyphs; i++) {
          advanceTable[i] = inStream.readSI16();
        }
        boundsTable = new Rect[numGlyphs];
        for (int i = 0; i < numGlyphs; i++) {
          boundsTable[i] = new Rect(inStream);
        }
      }
      int kerningCount = inStream.readUI16();
      if (kerningCount > 0) {
        kerningTable = new KerningRecord[kerningCount];
        for (int i = 0; i < kerningCount; i++) {
          kerningTable[i] = new KerningRecord(inStream, wideCodes);
        }
      }
    }
  }
}
