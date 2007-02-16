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

import com.jswiff.swfrecords.Shape;


/**
 * This tag's structure is identical to <code>DefineFont2</code>. The
 * difference is in the precision of the glyph definition within
 * glyphShapeTable: all EM square coordinates are multiplied by 20 before
 * being added to the tag, thus enabling substantial resolution increase.
 *
 * @since SWF 8.
 */
public final class DefineFont3 extends DefineFont2 {
  /**
   * @see DefineFont2#DefineFont2(int, String, Shape[], char[])
   */
  public DefineFont3(
    int characterId, String fontName, Shape[] glyphShapeTable, char[] codeTable) {
    super(characterId, fontName, glyphShapeTable, codeTable);
    code = TagConstants.DEFINE_FONT_3;
  }

  DefineFont3() {
    // empty
  }
}
