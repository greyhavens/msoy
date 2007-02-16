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

/**
 * This class contains constants regarding SWF tags.
 */
public final class TagConstants {
  // Tag types
  /** Tag code for DefineBits */
  public static final int DEFINE_BITS            = 6;
  /** Tag code for DefineBitsJpeg2 */
  public static final int DEFINE_BITS_JPEG_2     = 21;
  /** Tag code for DefineBitsJpeg3 */
  public static final int DEFINE_BITS_JPEG_3     = 35;
  /** Tag code for DefineBitsLossless */
  public static final int DEFINE_BITS_LOSSLESS   = 20;
  /** Tag code for DefineBitsLossless2 */
  public static final int DEFINE_BITS_LOSSLESS_2 = 36;
  /** Tag code for DefineButton */
  public static final int DEFINE_BUTTON          = 7;
  /** Tag code for DefineButton2 */
  public static final int DEFINE_BUTTON_2        = 34;
  /** Tag code for DefineButtonCXform */
  public static final int DEFINE_BUTTON_C_XFORM  = 23;
  /** Tag code for DefineButtonSound */
  public static final int DEFINE_BUTTON_SOUND    = 17;
  /** Tag code for DefineEditText */
  public static final int DEFINE_EDIT_TEXT       = 37;
  /** Tag code for DefineFont */
  public static final int DEFINE_FONT            = 10;
  /** Tag code for DefineFont2 */
  public static final int DEFINE_FONT_2          = 48;
  /** Tag code for DefineFont3 */
  public static final int DEFINE_FONT_3          = 75;
  /** Tag code for DefineFontInfo */
  public static final int DEFINE_FONT_INFO       = 13;
  /** Tag code for DefineFontInfo2 */
  public static final int DEFINE_FONT_INFO_2     = 62;
  /** Tag code for FlashTypeSettings */
  public static final short FLASHTYPE_SETTINGS   = 74;
  /** Tag code for DefineFontInfo3 */
  public static final int DEFINE_FONT_ALIGNMENT  = 73;
  /** Tag code for DefineMorphShape */
  public static final int DEFINE_MORPH_SHAPE     = 46;
  /** Tag code for DefineMorphShape2 */
  public static final int DEFINE_MORPH_SHAPE_2   = 84;
  /** Tag code for DefineShape */
  public static final int DEFINE_SHAPE           = 2;
  /** Tag code for DefineShape2 */
  public static final int DEFINE_SHAPE_2         = 22;
  /** Tag code for DefineShape3 */
  public static final int DEFINE_SHAPE_3         = 32;
  /** Tag code for DefineShape4 */
  public static final int DEFINE_SHAPE_4         = 83;
  /** Tag code for DefineSound */
  public static final int DEFINE_SOUND           = 14;
  /** Tag code for DefineSprite */
  public static final int DEFINE_SPRITE          = 39;
  /** Tag code for DefineText */
  public static final int DEFINE_TEXT            = 11;
  /** Tag code for DefineText2 */
  public static final int DEFINE_TEXT_2          = 33;
  /** Tag code for DefineVideoStream */
  public static final int DEFINE_VIDEO_STREAM    = 60;
  /** Tag code for DoAction */
  public static final int DO_ACTION              = 12;
  /** Tag code for DoInitAction */
  public static final int DO_INIT_ACTION         = 59;
  /** Tag code for EnableDebugger2 */
  public static final int ENABLE_DEBUGGER_2      = 64;
  /** Tag code for EnableDebugger */
  public static final int ENABLE_DEBUGGER        = 58;
  /** Tag code for End tag (used internally) */
  public static final int END                    = 0;
  /** Tag code for ExportAssets */
  public static final int EXPORT_ASSETS          = 56;
  /** Tag code for FileAttributes */
  public static final int FILE_ATTRIBUTES        = 69;
  /** Tag code for FrameLabel */
  public static final int FRAME_LABEL            = 43;
  /** Tag code for FreeCharacter */
  public static final int FREE_CHARACTER         = 3;
  /** Tag code for ImportAssets */
  public static final int IMPORT_ASSETS          = 57;
  /** Tag code for ImportAssets2 */
  public static final int IMPORT_ASSETS_2        = 71;
  /** Tag code for JpegTables */
  public static final int JPEG_TABLES            = 8;
  /** Tag code for Metadata */
  public static final int METADATA               = 77;
  /** Tag code for PlaceObject */
  public static final int PLACE_OBJECT           = 4;
  /** Tag code for PlaceObject2 */
  public static final int PLACE_OBJECT_2         = 26;
  /** Tag code for PlaceObject3 */
  public static final int PLACE_OBJECT_3         = 70;
  /** Tag code for Protect */
  public static final int PROTECT                = 24;
  /** Tag code for RemoveObject */
  public static final int REMOVE_OBJECT          = 5;
  /** Tag code for RemoveObject2 */
  public static final int REMOVE_OBJECT_2        = 28;
  /** Tag code for ScriptLimits */
  public static final int SCRIPT_LIMITS          = 65;
  /** Tag code for SetBackgroundColor */
  public static final int SET_BACKGROUND_COLOR   = 9;
  /** Tag code for SetTabIndex */
  public static final int SET_TAB_INDEX          = 66;
  /** Tag code for ShowFrame */
  public static final int SHOW_FRAME             = 1;
  /** Tag code for Scale9Grid */
  public static final short SCALE_9_GRID         = 78;
  /** Tag code for SoundStreamBlock */
  public static final int SOUND_STREAM_BLOCK     = 19;
  /** Tag code for SoundStreamHead */
  public static final int SOUND_STREAM_HEAD      = 18;
  /** Tag code for SoundStreamHead2 */
  public static final int SOUND_STREAM_HEAD_2    = 45;
  /** Tag code for StartSound */
  public static final int START_SOUND            = 15;
  /** Tag code for VideoFrame */
  public static final int VIDEO_FRAME            = 61;
  /** Tag code for malformed tag */
  public static final int MALFORMED              = -1;

  private TagConstants() {
    // prohibits instantiation
  }

  // undocumented tags
  // Debug tag: 63 (if enabling debug at saving, seems that EnableDebug2 and 63 are added)
  /**
   * Returns the tag name for a given tag code.
   *
   * @param code tag code
   *
   * @return corresponding tag name
   */
  public static String getTagName(int code) {
    String result;
    switch (code) {
      case DEFINE_BITS:
        result = "DefineBits";
        break;
      case DEFINE_BITS_JPEG_2:
        result = "DefineBitsJPEG2";
        break;
      case DEFINE_BITS_JPEG_3:
        result = "DefineBitsJPEG3";
        break;
      case DEFINE_BITS_LOSSLESS:
        result = "DefineBitsLossless";
        break;
      case DEFINE_BITS_LOSSLESS_2:
        result = "DefineBitsLossless2";
        break;
      case DEFINE_BUTTON:
        result = "DefineButton";
        break;
      case DEFINE_BUTTON_2:
        result = "DefineButton2";
        break;
      case DEFINE_BUTTON_C_XFORM:
        result = "DefineButtonCXform";
        break;
      case DEFINE_BUTTON_SOUND:
        result = "DefineButtonSound";
        break;
      case DEFINE_EDIT_TEXT:
        result = "DefineEditText";
        break;
      case DEFINE_FONT:
        result = "DefineFont";
        break;
      case DEFINE_FONT_2:
        result = "DefineFont2";
        break;
      case DEFINE_FONT_3:
        result = "DefineFont3";
        break;
      case DEFINE_FONT_INFO:
        result = "DefineFontInfo";
        break;
      case DEFINE_FONT_INFO_2:
        result = "DefineFontInfo2";
        break;
      case FLASHTYPE_SETTINGS:
        result = "FlashTypeSettings";
        break;
      case DEFINE_FONT_ALIGNMENT:
        result = "DefineFontInfo3";
        break;
      case DEFINE_MORPH_SHAPE:
        result = "DefineMorphShape";
        break;
      case DEFINE_MORPH_SHAPE_2:
        result = "DefineMorphShape2";
        break;
      case DEFINE_SHAPE:
        result = "DefineShape";
        break;
      case DEFINE_SHAPE_2:
        result = "DefineShape2";
        break;
      case DEFINE_SHAPE_3:
        result = "DefineShape3";
        break;
      case DEFINE_SHAPE_4:
        result = "DefineShape4";
        break;
      case DEFINE_SOUND:
        result = "DefineSound";
        break;
      case DEFINE_SPRITE:
        result = "DefineSprite";
        break;
      case DEFINE_TEXT:
        result = "DefineText";
        break;
      case DEFINE_TEXT_2:
        result = "DefineText2";
        break;
      case DEFINE_VIDEO_STREAM:
        result = "DefineVideoStream";
        break;
      case DO_ACTION:
        result = "DoAction";
        break;
      case DO_INIT_ACTION:
        result = "DoInitAction";
        break;
      case ENABLE_DEBUGGER_2:
        result = "EnableDebugger2";
        break;
      case ENABLE_DEBUGGER:
        result = "EnableDebugger";
        break;
      case END:
        result = "End";
        break;
      case EXPORT_ASSETS:
        result = "ExportAssets";
        break;
      case FILE_ATTRIBUTES:
        result = "FileAttributes";
        break;
      case FRAME_LABEL:
        result = "FrameLabel";
        break;
      case FREE_CHARACTER:
        result = "FreeCharacter";
        break;
      case IMPORT_ASSETS:
        result = "ImportAssets";
        break;
      case IMPORT_ASSETS_2:
        result = "ImportAssets2";
        break;
      case JPEG_TABLES:
        result = "JPEGTables";
        break;
      case METADATA:
        result = "Metadata";
        break;
      case PLACE_OBJECT:
        result = "PlaceObject";
        break;
      case PLACE_OBJECT_2:
        result = "PlaceObject2";
        break;
      case PLACE_OBJECT_3:
        result = "PlaceObject3";
        break;
      case PROTECT:
        result = "Protect";
        break;
      case REMOVE_OBJECT:
        result = "RemoveObject";
        break;
      case REMOVE_OBJECT_2:
        result = "RemoveObject2";
        break;
      case SCRIPT_LIMITS:
        result = "ScriptLimits";
        break;
      case SET_BACKGROUND_COLOR:
        result = "SetBackgroundColor";
        break;
      case SET_TAB_INDEX:
        result = "SetTabIndex";
        break;
      case SHOW_FRAME:
        result = "ShowFrame";
        break;
      case SCALE_9_GRID:
        result = "Scale9Grid";
        break;
      case SOUND_STREAM_BLOCK:
        result = "SoundStreamBlock";
        break;
      case SOUND_STREAM_HEAD:
        result = "SoundStreamHead";
        break;
      case SOUND_STREAM_HEAD_2:
        result = "SoundStreamHead2";
        break;
      case START_SOUND:
        result = "StartSound";
        break;
      case VIDEO_FRAME:
        result = "VideoFrame";
        break;
      case MALFORMED:
        result = "Malformed tag";
        break;
      default:
        result = "Unknown tag";
    }
    return result;
  }
}
