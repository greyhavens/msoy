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

import java.io.IOException;


/**
 * This class contains methods used for parsing tag headers and tags.
 */
public final class TagReader {
  private TagReader() {
    // prohibits instantiation
  }

  /**
   * Reads a tag from a data buffer. The tag header must be parsed before
   * invoking this method.
   *
   * @param header tag header
   * @param tagData data buffer containing the tag to be read
   * @param swfVersion flash version (from the SWF file header)
   * @param japanese specifies whether japanese encoding is to be used for strings
   *
   * @return the read tag
   *
   * @throws IOException if I/O problems occur
   */
  public static Tag readTag(TagHeader header, byte[] tagData, short swfVersion, boolean japanese)
    throws IOException {
    Tag tag;
    switch (header.getCode()) {
      case TagConstants.DEFINE_BITS:
        tag = new DefineBits();
        break;
      case TagConstants.DEFINE_BITS_JPEG_2:
        tag = new DefineBitsJPEG2();
        break;
      case TagConstants.DEFINE_BITS_JPEG_3:
        tag = new DefineBitsJPEG3();
        break;
      case TagConstants.DEFINE_BITS_LOSSLESS:
        tag = new DefineBitsLossless();
        break;
      case TagConstants.DEFINE_BITS_LOSSLESS_2:
        tag = new DefineBitsLossless2();
        break;
      case TagConstants.DEFINE_BUTTON:
        tag = new DefineButton();
        break;
      case TagConstants.DEFINE_BUTTON_2:
        tag = new DefineButton2();
        break;
      case TagConstants.DEFINE_BUTTON_C_XFORM:
        tag = new DefineButtonCXform();
        break;
      case TagConstants.DEFINE_BUTTON_SOUND:
        tag = new DefineButtonSound();
        break;
      case TagConstants.DEFINE_EDIT_TEXT:
        tag = new DefineEditText();
        break;
      case TagConstants.DEFINE_FONT:
        tag = new DefineFont();
        break;
      case TagConstants.DEFINE_FONT_2:
        tag = new DefineFont2();
        break;
      case TagConstants.DEFINE_FONT_3:
        tag = new DefineFont3();
        break;
      case TagConstants.DEFINE_FONT_INFO:
        tag = new DefineFontInfo();
        break;
      case TagConstants.DEFINE_FONT_INFO_2:
        tag = new DefineFontInfo2();
        break;
      case TagConstants.FLASHTYPE_SETTINGS:
        tag = new FlashTypeSettings();
        break;
      case TagConstants.DEFINE_FONT_ALIGNMENT:
        tag = new DefineFontAlignment();
        break;
      case TagConstants.DEFINE_MORPH_SHAPE:
        tag = new DefineMorphShape();
        break;
      case TagConstants.DEFINE_MORPH_SHAPE_2:
        tag = new DefineMorphShape2();
        break;
      case TagConstants.DEFINE_SHAPE:
        tag = new DefineShape();
        break;
      case TagConstants.DEFINE_SHAPE_2:
        tag = new DefineShape2();
        break;
      case TagConstants.DEFINE_SHAPE_3:
        tag = new DefineShape3();
        break;
      case TagConstants.DEFINE_SHAPE_4:
        tag = new DefineShape4();
        break;
      case TagConstants.DEFINE_SOUND:
        tag = new DefineSound();
        break;
      case TagConstants.DEFINE_SPRITE:
        tag = new DefineSprite();
        break;
      case TagConstants.DEFINE_TEXT:
        tag = new DefineText();
        break;
      case TagConstants.DEFINE_TEXT_2:
        tag = new DefineText2();
        break;
      case TagConstants.DEFINE_VIDEO_STREAM:
        tag = new DefineVideoStream();
        break;
      case TagConstants.DO_ACTION:
        tag = new DoAction();
        break;
      case TagConstants.DO_INIT_ACTION:
        tag = new DoInitAction();
        break;
      case TagConstants.ENABLE_DEBUGGER_2:
        tag = new EnableDebugger2();
        break;
      case TagConstants.ENABLE_DEBUGGER:
        tag = new EnableDebugger();
        break;
      case TagConstants.EXPORT_ASSETS:
        tag = new ExportAssets();
        break;
      case TagConstants.FILE_ATTRIBUTES:
        tag = new FileAttributes();
        break;
      case TagConstants.FRAME_LABEL:
        tag = new FrameLabel();
        break;
      case TagConstants.IMPORT_ASSETS:
        tag = new ImportAssets();
        break;
      case TagConstants.IMPORT_ASSETS_2:
        tag = new ImportAssets2();
        break;
      case TagConstants.JPEG_TABLES:
        tag = new JPEGTables();
        break;
      case TagConstants.METADATA:
        tag = new Metadata();
        break;
      case TagConstants.PLACE_OBJECT:
        tag = new PlaceObject();
        break;
      case TagConstants.PLACE_OBJECT_2:
        tag = new PlaceObject2();
        break;
      case TagConstants.PLACE_OBJECT_3:
        tag = new PlaceObject3();
        break;
      case TagConstants.PROTECT:
        tag = new Protect();
        break;
      case TagConstants.REMOVE_OBJECT:
        tag = new RemoveObject();
        break;
      case TagConstants.REMOVE_OBJECT_2:
        tag = new RemoveObject2();
        break;
      case TagConstants.SCRIPT_LIMITS:
        tag = new ScriptLimits();
        break;
      case TagConstants.SET_BACKGROUND_COLOR:
        tag = new SetBackgroundColor();
        break;
      case TagConstants.SET_TAB_INDEX:
        tag = new SetTabIndex();
        break;
      case TagConstants.SHOW_FRAME:
        tag = new ShowFrame();
        break;
      case TagConstants.SCALE_9_GRID:
        tag = new Scale9Grid();
        break;
      case TagConstants.SOUND_STREAM_BLOCK:
        tag = new SoundStreamBlock();
        break;
      case TagConstants.SOUND_STREAM_HEAD:
        tag = new SoundStreamHead();
        break;
      case TagConstants.SOUND_STREAM_HEAD_2:
        tag = new SoundStreamHead2();
        break;
      case TagConstants.START_SOUND:
        tag = new StartSound();
        break;
      case TagConstants.VIDEO_FRAME:
        tag = new VideoFrame();
        break;
      default:
        tag = new UnknownTag();
    }
    tag.setCode(header.getCode());
    tag.setSWFVersion(swfVersion);
    tag.setJapanese(japanese);
    tag.setData(tagData);
    return tag;
  }

  /**
   * Reads a tag from a bit stream as raw data. The tag header must be read
   * before invoking this method.
   *
   * @param stream source bit stream
   * @param header tag header
   *
   * @return tag as data buffer
   *
   * @throws IOException if an I/O error occured
   */
  public static byte[] readTagData(InputBitStream stream, TagHeader header)
    throws IOException {
    return stream.readBytes(header.getLength());
  }

  /**
   * Reads a tag header from a bit stream.
   *
   * @param stream source bit stream
   *
   * @return the parsed tag header
   *
   * @throws IOException if an I/O error occured
   */
  public static TagHeader readTagHeader(InputBitStream stream)
    throws IOException {
    return new TagHeader(stream);
  }

  /*
   * Reads a tag from a bit stream.
   */
  static Tag readTag(InputBitStream stream, short swfVersion, boolean shiftJIS)
    throws IOException {
    TagHeader header = new TagHeader(stream);
    byte[] tagData   = stream.readBytes(header.getLength());
    return readTag(header, tagData, swfVersion, shiftJIS);
  }
}
