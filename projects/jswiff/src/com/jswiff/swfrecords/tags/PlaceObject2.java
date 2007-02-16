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
import com.jswiff.swfrecords.CXformWithAlpha;
import com.jswiff.swfrecords.ClipActions;
import com.jswiff.swfrecords.Matrix;

import java.io.IOException;


/**
 * <p>
 * With this tag, an instance of a character can be added to the display list.
 * Besides, unlike <code>PlaceObject</code>, it can modify the attributes of a
 * character that is already on the display list.
 * </p>
 * 
 * <p>
 * The only mandatory attribute is the character depth. A character that is
 * already on the display list can be identified by its depth alone, as there
 * can be only one character at a given depth. If a new character is added to
 * the display list, the character ID is needed.
 * </p>
 * 
 * <p>
 * If the <code>move</code> flag is set, the character at the given depth is
 * removed. If no character ID is set, the removed character is redisplayed at
 * the same depth with new attributes. If the character ID is set, the
 * corresponding character replaces the removed one.
 * </p>
 * 
 * <p>
 * A transform matrix and a color tranform can be specified in order to
 * determine the position, rotation, scale and color of the character to be
 * displayed.
 * </p>
 * 
 * <p>
 * The morph ratio applies to characters defined with
 * <code>DefineMorphShape</code> and specifies how far the morph has
 * progressed.
 * </p>
 * 
 * <p>
 * A (non-zero) clip depth indicates that the character is a clipping character
 * which masks depths up to and including the specified value (e.g. if a
 * character was placed at depth 1 with a clip depth of 4, all depths above 1,
 * up to and including depth 4, will be masked by the shape placed at depth 1;
 * characters placed at depths above 4 will not be masked).
 * </p>
 * 
 * <p>
 * The character instance can be given a name which it can later be referenced
 * by (e.g. within <code>With</code>).
 * </p>
 * 
 * <p>
 * Finally, if the character to be placed is a sprite, one or more event
 * handlers (clip actions) can be defined.
 * </p>
 *
 * @see PlaceObject
 * @see DefineMorphShape
 * @since SWF 3
 */
public final class PlaceObject2 extends Tag {
  private boolean move;
  private int depth;
  private int characterId;
  private Matrix matrix;
  private CXformWithAlpha colorTransform;
  private int ratio;
  private String name;
  private int clipDepth;
  private ClipActions clipActions;
  private boolean hasClipActions;
  private boolean hasClipDepth;
  private boolean hasName;
  private boolean hasRatio;
  private boolean hasColorTransform;
  private boolean hasMatrix;
  private boolean hasCharacter;

  /**
   * Creates a new PlaceObject2 tag.
   *
   * @param depth depth the character is placed at
   */
  public PlaceObject2(int depth) {
    code         = TagConstants.PLACE_OBJECT_2;
    this.depth   = depth;
  }

  PlaceObject2() {
    // empty
  }

  /**
   * Sets the character ID. If this ID is set, the corresponding character is
   * displayed at the depth specified with the constructor.
   *
   * @param characterId The characterId to set.
   */
  public void setCharacterId(int characterId) {
    this.characterId   = characterId;
    hasCharacter       = true;
  }

  /**
   * Returns the character ID. If this ID is set, the corresponding character
   * is displayed at the depth specified with the constructor. Check with
   * <code>hasCharacter()</code> if set.
   *
   * @return Returns the characterId.
   */
  public int getCharacterId() {
    return characterId;
  }

  /**
   * Sets the event handlers (only for sprite characters).
   *
   * @param clipActions event handlers
   */
  public void setClipActions(ClipActions clipActions) {
    this.clipActions   = clipActions;
    hasClipActions     = (clipActions != null);
  }

  /**
   * Returns the event handlers (only for sprite characters). Check with
   * <code>hasClipActions()</code> if set.
   *
   * @return Returns the clipActions.
   */
  public ClipActions getClipActions() {
    return clipActions;
  }

  /**
   * Sets the clip depth, indicating that the character is a clipping character
   * which masks characters at depths up to and including the specified clip
   * depth
   *
   * @param clipDepth the clip depth
   */
  public void setClipDepth(int clipDepth) {
    this.clipDepth   = clipDepth;
    hasClipDepth     = true;
  }

  /**
   * Returs the clip depth (which indicates that the character is a clipping
   * character masking characters at depths up to and including the specified
   * clip depth). Check with <code>hasclipDepth()</code> if set.
   *
   * @return clip depth of character
   */
  public int getClipDepth() {
    return clipDepth;
  }

  /**
   * Sets the color transform, allowing color effects to be applied to the
   * character to be displayed.
   *
   * @param colorTransform a color transform
   */
  public void setColorTransform(CXformWithAlpha colorTransform) {
    this.colorTransform   = colorTransform;
    hasColorTransform     = (colorTransform != null);
  }

  /**
   * Returns the color transform which allows color effects to be applied to
   * the character to be displayed. Check with
   * <code>hasColorTransform()</code> if set.
   *
   * @return Returns the colorTransform.
   */
  public CXformWithAlpha getColorTransform() {
    return colorTransform;
  }

  /**
   * Sets the depth the character will be displayed at.
   *
   * @param depth display depth
   */
  public void setDepth(int depth) {
    this.depth = depth;
  }

  /**
   * Returns the depth the character will be displayed at.
   *
   * @return display depth
   */
  public int getDepth() {
    return depth;
  }

  /**
   * Sets the tranform matrix, specifying position, scale, rotation etc. of the
   * character to be displayed.
   *
   * @param matrix transform matrix
   */
  public void setMatrix(Matrix matrix) {
    this.matrix   = matrix;
    hasMatrix     = (matrix != null);
  }

  /**
   * Returns the tranform matrix, which specifies position, scale, rotation
   * etc. of the character to be displayed. Check with
   * <code>hasMatrix()</code> if set.
   *
   * @return transform matrix
   */
  public Matrix getMatrix() {
    return matrix;
  }

  /**
   * <p>
   * Sets the move flag. If set, the character at the given depth is removed.
   * It is replaced either by a modified instance of the removed character or
   * (if a character ID is specified) by a new character.
   * </p>
   * 
   * <p>
   * Mainly used for moving a character: first frame specifies depth, character
   * ID and initial matrix. Subsequent frames have the move flag set for this
   * depth and replace the translate values of the matrix with new ones.
   * </p>
   */
  public void setMove() {
    move = true;
  }

  /**
   * Sets or clears the move flag.
   *
   * @param move value of move flag
   *
   * @see PlaceObject2#setMove()
   */
  public void setMove(boolean move) {
    this.move = move;
  }

  /**
   * Checks the move flag. If set, the character at the given depth is removed.
   * It is replaced either by a modified instance of the removed character or
   * (if a character ID is specified) by a new character.
   *
   * @return <code>true</code> if move flag set, else <code>false</code>
   */
  public boolean isMove() {
    return move;
  }

  /**
   * Assigns a name to the instance of the character to be placed, in order to
   * be able to reference this instance by the assigend name (e.g. within
   * <code>With</code>).
   *
   * @param name instance name
   */
  public void setName(String name) {
    this.name = name;
    hasName = (name != null);
  }

  /**
   * Returns the name assigned to the instance of the character to be placed,
   * in order to be able to reference this instance by the assigend name (e.g.
   * within <code>With</code>). Check with <code>hasName()</code> if set.
   *
   * @return instance name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the morph ratio which indicates how far the morph has progressed
   * (only for characters defined with <code>DefineMorphShape</code>). Values
   * between 0 and 65535 are permitted. A ratio of 0 displays the character at
   * morph start, 65535 causes the character to be displayed at the end of the
   * morph. Values between 0 and 65535 cause the Flash Player to interpolate
   * between start and end shapes.
   *
   * @param ratio morph ratio
   */
  public void setRatio(int ratio) {
    if (ratio < 0) {
      this.ratio = 0;
    } else if (ratio > 65535) {
      this.ratio = 65535;
    } else {
      this.ratio = ratio;
    }
    hasRatio = true;
  }

  /**
   * <p>
   * Sets the morph ratio which indicates how far the morph has progressed
   * (only for characters defined with <code>DefineMorphShape</code>). Values
   * between 0 and 65535 are returned. A ratio of 0 displays the character at
   * morph start, 65535 causes the character to be displayed at the end of the
   * morph. Values between 0 and 65535 cause the Flash Player to interpolate
   * between start and end shapes.
   * </p>
   * 
   * <p>
   * Check with <code>hasRatio()</code> if set.
   * </p>
   *
   * @return Returns the ratio.
   */
  public int getRatio() {
    return ratio;
  }

  /**
   * Checks whether the character ID is set.
   *
   * @return <code>true</code> if character ID set, else <code>false</code>
   */
  public boolean hasCharacter() {
    return hasCharacter;
  }

  /**
   * Checks whether clip actions (sprite event handlers) have been specified.
   *
   * @return <code>true</code> if clip actions set, else <code>false</code>
   */
  public boolean hasClipActions() {
    return hasClipActions;
  }

  /**
   * Checks if the value of the clip depth was set.
   *
   * @return <code>true</code> if clip depth set, else <code>false</code>
   */
  public boolean hasClipDepth() {
    return hasClipDepth;
  }

  /**
   * Checks whether the color transform is specified.
   *
   * @return <code>true</code> if color transform set, else <code>false</code>
   */
  public boolean hasColorTransform() {
    return hasColorTransform;
  }

  /**
   * Checks if a transform matrix is specified.
   *
   * @return <code>true</code> if transform matrix set, else <code>false</code>
   */
  public boolean hasMatrix() {
    return hasMatrix;
  }

  /**
   * Checks if a name is assigned to the character instance to be displayed.
   *
   * @return <code>true</code> if character instance name set, else
   *         <code>false</code>
   */
  public boolean hasName() {
    return hasName;
  }

  /**
   * Checks if the morph ratio is set.
   *
   * @return <code>true</code> if ratio set, else <code>false</code>
   */
  public boolean hasRatio() {
    return hasRatio;
  }

  protected void writeData(OutputBitStream outStream) throws IOException {
    outStream.writeBooleanBit(hasClipActions);
    outStream.writeBooleanBit(hasClipDepth);
    outStream.writeBooleanBit(hasName);
    outStream.writeBooleanBit(hasRatio);
    outStream.writeBooleanBit(hasColorTransform);
    outStream.writeBooleanBit(hasMatrix);
    outStream.writeBooleanBit(hasCharacter);
    outStream.writeBooleanBit(move);
    outStream.writeUI16(depth);
    if (hasCharacter) {
      outStream.writeUI16(characterId);
    }
    if (hasMatrix) {
      matrix.write(outStream);
    }
    if (hasColorTransform) {
      colorTransform.write(outStream);
    }
    if (hasRatio) {
      outStream.writeUI16(ratio);
    }
    if (hasName) {
      outStream.writeString(name);
    }
    if (hasClipDepth) {
      outStream.writeUI16(clipDepth);
    }
    if (hasClipActions) {
      clipActions.write(outStream, getSWFVersion());
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
    hasClipActions      = inStream.readBooleanBit();
    hasClipDepth        = inStream.readBooleanBit();
    hasName             = inStream.readBooleanBit();
    hasRatio            = inStream.readBooleanBit();
    hasColorTransform   = inStream.readBooleanBit();
    hasMatrix           = inStream.readBooleanBit();
    hasCharacter        = inStream.readBooleanBit();
    move                = inStream.readBooleanBit();
    depth               = inStream.readUI16();
    if (hasCharacter) {
      characterId = inStream.readUI16();
    }
    if (hasMatrix) {
      matrix = new Matrix(inStream);
    }
    if (hasColorTransform) {
      colorTransform = new CXformWithAlpha(inStream);
    }
    if (hasRatio) {
      ratio = inStream.readUI16();
    }
    if (hasName) {
      name = inStream.readString();
    }
    if (hasClipDepth) {
      clipDepth = inStream.readUI16();
    }
    if ((getSWFVersion() >= 5) && hasClipActions) {
      clipActions = new ClipActions(inStream, getSWFVersion());
    }
  }
}
