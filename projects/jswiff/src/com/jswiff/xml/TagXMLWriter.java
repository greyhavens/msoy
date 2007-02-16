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

package com.jswiff.xml;

import com.jswiff.swfrecords.BlendMode;
import com.jswiff.swfrecords.ButtonCondAction;
import com.jswiff.swfrecords.ButtonRecord;
import com.jswiff.swfrecords.CXform;
import com.jswiff.swfrecords.KerningRecord;
import com.jswiff.swfrecords.LangCode;
import com.jswiff.swfrecords.MorphFillStyles;
import com.jswiff.swfrecords.MorphLineStyles;
import com.jswiff.swfrecords.Shape;
import com.jswiff.swfrecords.TextRecord;
import com.jswiff.swfrecords.tags.DefineBits;
import com.jswiff.swfrecords.tags.DefineBitsJPEG2;
import com.jswiff.swfrecords.tags.DefineBitsJPEG3;
import com.jswiff.swfrecords.tags.DefineBitsLossless;
import com.jswiff.swfrecords.tags.DefineBitsLossless2;
import com.jswiff.swfrecords.tags.DefineButton;
import com.jswiff.swfrecords.tags.DefineButton2;
import com.jswiff.swfrecords.tags.DefineButtonCXform;
import com.jswiff.swfrecords.tags.DefineButtonSound;
import com.jswiff.swfrecords.tags.DefineEditText;
import com.jswiff.swfrecords.tags.DefineFont;
import com.jswiff.swfrecords.tags.DefineFont2;
import com.jswiff.swfrecords.tags.DefineFont3;
import com.jswiff.swfrecords.tags.DefineFontAlignment;
import com.jswiff.swfrecords.tags.DefineFontInfo;
import com.jswiff.swfrecords.tags.DefineFontInfo2;
import com.jswiff.swfrecords.tags.DefineMorphShape;
import com.jswiff.swfrecords.tags.DefineMorphShape2;
import com.jswiff.swfrecords.tags.DefineShape;
import com.jswiff.swfrecords.tags.DefineShape2;
import com.jswiff.swfrecords.tags.DefineShape3;
import com.jswiff.swfrecords.tags.DefineShape4;
import com.jswiff.swfrecords.tags.DefineSound;
import com.jswiff.swfrecords.tags.DefineSprite;
import com.jswiff.swfrecords.tags.DefineText;
import com.jswiff.swfrecords.tags.DefineText2;
import com.jswiff.swfrecords.tags.DefineVideoStream;
import com.jswiff.swfrecords.tags.DoAction;
import com.jswiff.swfrecords.tags.DoInitAction;
import com.jswiff.swfrecords.tags.EnableDebugger;
import com.jswiff.swfrecords.tags.EnableDebugger2;
import com.jswiff.swfrecords.tags.ExportAssets;
import com.jswiff.swfrecords.tags.FlashTypeSettings;
import com.jswiff.swfrecords.tags.FrameLabel;
import com.jswiff.swfrecords.tags.FreeCharacter;
import com.jswiff.swfrecords.tags.ImportAssets;
import com.jswiff.swfrecords.tags.ImportAssets2;
import com.jswiff.swfrecords.tags.JPEGTables;
import com.jswiff.swfrecords.tags.MalformedTag;
import com.jswiff.swfrecords.tags.PlaceObject;
import com.jswiff.swfrecords.tags.PlaceObject2;
import com.jswiff.swfrecords.tags.PlaceObject3;
import com.jswiff.swfrecords.tags.Protect;
import com.jswiff.swfrecords.tags.RemoveObject;
import com.jswiff.swfrecords.tags.RemoveObject2;
import com.jswiff.swfrecords.tags.Scale9Grid;
import com.jswiff.swfrecords.tags.ScriptLimits;
import com.jswiff.swfrecords.tags.SetTabIndex;
import com.jswiff.swfrecords.tags.ShowFrame;
import com.jswiff.swfrecords.tags.SoundStreamBlock;
import com.jswiff.swfrecords.tags.SoundStreamHead;
import com.jswiff.swfrecords.tags.SoundStreamHead2;
import com.jswiff.swfrecords.tags.StartSound;
import com.jswiff.swfrecords.tags.Tag;
import com.jswiff.swfrecords.tags.TagConstants;
import com.jswiff.swfrecords.tags.UnknownTag;
import com.jswiff.swfrecords.tags.VideoFrame;
import com.jswiff.util.Base64;
import com.jswiff.util.StringUtilities;

import org.dom4j.Element;

import java.util.Iterator;


/*
 * Writes SWF tags to XML.
 */
class TagXMLWriter {
  static void writeTag(Element parentElement, Tag tag) {
    int tagCode = tag.getCode();
    switch (tagCode) {
      case TagConstants.DEFINE_BITS:
        writeDefineBits(parentElement, (DefineBits) tag);
        break;
      case TagConstants.DEFINE_BITS_JPEG_2:
        writeDefineBitsJPEG2(parentElement, (DefineBitsJPEG2) tag);
        break;
      case TagConstants.DEFINE_BITS_JPEG_3:
        writeDefineBitsJPEG3(parentElement, (DefineBitsJPEG3) tag);
        break;
      case TagConstants.DEFINE_BITS_LOSSLESS:
        writeDefineBitsLossless(parentElement, (DefineBitsLossless) tag);
        break;
      case TagConstants.DEFINE_BITS_LOSSLESS_2:
        writeDefineBitsLossless2(parentElement, (DefineBitsLossless2) tag);
        break;
      case TagConstants.DEFINE_BUTTON:
        writeDefineButton(parentElement, (DefineButton) tag);
        break;
      case TagConstants.DEFINE_BUTTON_2:
        writeDefineButton2(parentElement, (DefineButton2) tag);
        break;
      case TagConstants.DEFINE_BUTTON_C_XFORM:
        writeDefineButtonCXform(parentElement, (DefineButtonCXform) tag);
        break;
      case TagConstants.DEFINE_BUTTON_SOUND:
        writeDefineButtonSound(parentElement, (DefineButtonSound) tag);
        break;
      case TagConstants.DEFINE_EDIT_TEXT:
        writeDefineEditText(parentElement, (DefineEditText) tag);
        break;
      case TagConstants.DEFINE_FONT:
        writeDefineFont(parentElement, (DefineFont) tag);
        break;
      case TagConstants.DEFINE_FONT_2:
        writeDefineFont2(parentElement, (DefineFont2) tag);
        break;
      case TagConstants.DEFINE_FONT_3:
        writeDefineFont3(parentElement, (DefineFont3) tag);
        break;
      case TagConstants.DEFINE_FONT_INFO:
        writeDefineFontInfo(parentElement, (DefineFontInfo) tag);
        break;
      case TagConstants.DEFINE_FONT_INFO_2:
        writeDefineFontInfo2(parentElement, (DefineFontInfo2) tag);
        break;
      case TagConstants.DEFINE_FONT_ALIGNMENT:
        writeDefineFontAlignment(parentElement, (DefineFontAlignment) tag);
        break;
      case TagConstants.FLASHTYPE_SETTINGS:
        writeFlashTypeSettings(parentElement, (FlashTypeSettings) tag);
        break;
      case TagConstants.DEFINE_MORPH_SHAPE:
        writeDefineMorphShape(parentElement, (DefineMorphShape) tag);
        break;
      case TagConstants.DEFINE_MORPH_SHAPE_2:
        writeDefineMorphShape2(parentElement, (DefineMorphShape2) tag);
        break;
      case TagConstants.DEFINE_SHAPE:
        writeDefineShape(parentElement, (DefineShape) tag);
        break;
      case TagConstants.DEFINE_SHAPE_2:
        writeDefineShape2(parentElement, (DefineShape2) tag);
        break;
      case TagConstants.DEFINE_SHAPE_3:
        writeDefineShape3(parentElement, (DefineShape3) tag);
        break;
      case TagConstants.DEFINE_SHAPE_4:
        writeDefineShape4(parentElement, (DefineShape4) tag);
        break;
      case TagConstants.DEFINE_SOUND:
        writeDefineSound(parentElement, (DefineSound) tag);
        break;
      case TagConstants.DEFINE_SPRITE:
        writeDefineSprite(parentElement, (DefineSprite) tag);
        break;
      case TagConstants.DEFINE_TEXT:
        writeDefineText(parentElement, (DefineText) tag);
        break;
      case TagConstants.DEFINE_TEXT_2:
        writeDefineText2(parentElement, (DefineText2) tag);
        break;
      case TagConstants.DEFINE_VIDEO_STREAM:
        writeDefineVideoStream(parentElement, (DefineVideoStream) tag);
        break;
      case TagConstants.DO_ACTION:
        writeDoAction(parentElement, (DoAction) tag);
        break;
      case TagConstants.DO_INIT_ACTION:
        writeDoInitAction(parentElement, (DoInitAction) tag);
        break;
      case TagConstants.ENABLE_DEBUGGER:
        writeEnableDebugger(parentElement, (EnableDebugger) tag);
        break;
      case TagConstants.ENABLE_DEBUGGER_2:
        writeEnableDebugger2(parentElement, (EnableDebugger2) tag);
        break;
      case TagConstants.EXPORT_ASSETS:
        writeExportAssets(parentElement, (ExportAssets) tag);
        break;
      case TagConstants.FRAME_LABEL:
        writeFrameLabel(parentElement, (FrameLabel) tag);
        break;
      case TagConstants.FREE_CHARACTER:
        writeFreeCharacter(parentElement, (FreeCharacter) tag);
        break;
      case TagConstants.IMPORT_ASSETS:
        writeImportAssets(parentElement, (ImportAssets) tag);
        break;
      case TagConstants.IMPORT_ASSETS_2:
        writeImportAssets2(parentElement, (ImportAssets2) tag);
        break;
      case TagConstants.JPEG_TABLES:
        writeJPEGTables(parentElement, (JPEGTables) tag);
        break;
      case TagConstants.MALFORMED:
        writeMalformedTag(parentElement, (MalformedTag) tag);
        break;
      case TagConstants.PLACE_OBJECT:
        writePlaceObject(parentElement, (PlaceObject) tag);
        break;
      case TagConstants.PLACE_OBJECT_2:
        writePlaceObject2(parentElement, (PlaceObject2) tag);
        break;
      case TagConstants.PLACE_OBJECT_3:
        writePlaceObject3(parentElement, (PlaceObject3) tag);
        break;
      case TagConstants.PROTECT:
        writeProtect(parentElement, (Protect) tag);
        break;
      case TagConstants.REMOVE_OBJECT:
        writeRemoveObject(parentElement, (RemoveObject) tag);
        break;
      case TagConstants.REMOVE_OBJECT_2:
        writeRemoveObject2(parentElement, (RemoveObject2) tag);
        break;
      case TagConstants.SCRIPT_LIMITS:
        writeScriptLimits(parentElement, (ScriptLimits) tag);
        break;
      case TagConstants.SET_TAB_INDEX:
        writeSetTabIndex(parentElement, (SetTabIndex) tag);
        break;
      case TagConstants.SHOW_FRAME:
        writeShowFrame(parentElement, (ShowFrame) tag);
        break;
      case TagConstants.SCALE_9_GRID:
        writeScale9Grid(parentElement, (Scale9Grid) tag);
        break;
      case TagConstants.SOUND_STREAM_BLOCK:
        writeSoundStreamBlock(parentElement, (SoundStreamBlock) tag);
        break;
      case TagConstants.SOUND_STREAM_HEAD:
        writeSoundStreamHead(parentElement, (SoundStreamHead) tag);
        break;
      case TagConstants.SOUND_STREAM_HEAD_2:
        writeSoundStreamHead2(parentElement, (SoundStreamHead2) tag);
        break;
      case TagConstants.START_SOUND:
        writeStartSound(parentElement, (StartSound) tag);
        break;
      case TagConstants.VIDEO_FRAME:
        writeVideoFrame(parentElement, (VideoFrame) tag);
        break;
      default:
        writeUnknownTag(parentElement, (UnknownTag) tag);
    }
  }

  private static String getSoundFormatString(byte format) {
    switch (format) {
      case DefineSound.FORMAT_ADPCM:
        return "adpcm";
      case DefineSound.FORMAT_MP3:
        return "mp3";
      case DefineSound.FORMAT_NELLYMOSER:
        return "nellymoser";
      case DefineSound.FORMAT_UNCOMPRESSED:
        return "uncompressed";
      case DefineSound.FORMAT_UNCOMPRESSED_LITTLE_ENDIAN:
        return "uncompressedle";
      default:
        throw new IllegalArgumentException("Illegal sound format: " + format);
    }
  }

  private static String getSoundRateString(byte rate) {
    switch (rate) {
      case DefineSound.RATE_5500_HZ:
        return "5500";
      case DefineSound.RATE_11000_HZ:
        return "11000";
      case DefineSound.RATE_22000_HZ:
        return "22000";
      case DefineSound.RATE_44000_HZ:
        return "44000";
      default:
        throw new IllegalArgumentException("Illegal sound rate: " + rate);
    }
  }

  private static void writeDefineBits(Element parentElement, DefineBits tag) {
    Element element = parentElement.addElement("definebits");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    element.addElement("jpegdata").addText(Base64.encode(tag.getJpegData()));
  }

  private static void writeDefineBitsJPEG2(
    Element parentElement, DefineBitsJPEG2 tag) {
    Element element = parentElement.addElement("definebitsjpeg2");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    element.addElement("jpegdata").addText(Base64.encode(tag.getJpegData()));
  }

  private static void writeDefineBitsJPEG3(
    Element parentElement, DefineBitsJPEG3 tag) {
    Element element = parentElement.addElement("definebitsjpeg3");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    element.addElement("jpegdata").addText(Base64.encode(tag.getJpegData()));
    element.addElement("alphadata").addText(
      Base64.encode(tag.getBitmapAlphaData()));
  }

  private static void writeDefineBitsLossless(
    Element parentElement, DefineBitsLossless tag) {
    Element element = parentElement.addElement("definebitslossless");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    String format;
    switch (tag.getFormat()) {
      case DefineBitsLossless.FORMAT_8_BIT_COLORMAPPED:
        format = "8bit";
        break;
      case DefineBitsLossless.FORMAT_15_BIT_RGB:
        format = "15bit";
        break;
      case DefineBitsLossless.FORMAT_24_BIT_RGB:
        format = "24bit";
        break;
      default:
        throw new IllegalArgumentException(
          "Illegal lossless bitmap format: " + tag.getFormat());
    }
    element.addAttribute("format", format);
    element.addAttribute("width", Integer.toString(tag.getWidth()));
    element.addAttribute("height", Integer.toString(tag.getHeight()));
    RecordXMLWriter.writeZlibBitmapData(element, tag.getZlibBitmapData());
  }

  private static void writeDefineBitsLossless2(
    Element parentElement, DefineBitsLossless2 tag) {
    Element element = parentElement.addElement("definebitslossless2");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    String format;
    switch (tag.getFormat()) {
      case DefineBitsLossless2.FORMAT_8_BIT_COLORMAPPED:
        format = "8bit";
        break;
      case DefineBitsLossless2.FORMAT_32_BIT_RGBA:
        format = "32bit";
        break;
      default:
        throw new IllegalArgumentException(
          "Illegal lossless bitmap format: " + tag.getFormat());
    }
    element.addAttribute("format", format);
    element.addAttribute("width", Integer.toString(tag.getWidth()));
    element.addAttribute("height", Integer.toString(tag.getHeight()));
    RecordXMLWriter.writeZlibBitmapData(element, tag.getZlibBitmapData());
  }

  private static void writeDefineButton(
    Element parentElement, DefineButton tag) {
    Element element = parentElement.addElement("definebutton");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    Element charsElement      = element.addElement("chars");
    ButtonRecord[] characters = tag.getCharacters();
    for (int i = 0; i < characters.length; i++) {
      RecordXMLWriter.writeButtonRecord(charsElement, characters[i]);
    }
    RecordXMLWriter.writeActionBlock(element, tag.getActions());
  }

  private static void writeDefineButton2(
    Element parentElement, DefineButton2 tag) {
    Element element = parentElement.addElement("definebutton2");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    if (tag.isTrackAsMenu()) {
      element.addAttribute("trackasmenu", "true");
    }
    Element charsElement      = element.addElement("chars");
    ButtonRecord[] characters = tag.getCharacters();
    for (int i = 0; i < characters.length; i++) {
      RecordXMLWriter.writeButtonRecord(charsElement, characters[i]);
    }
    Element actionsElement     = element.addElement("actions");
    ButtonCondAction[] actions = tag.getActions();
    if (actions != null) {
      for (int i = 0; i < actions.length; i++) {
        RecordXMLWriter.writeButtonCondAction(actionsElement, actions[i]);
      }
    }
  }

  private static void writeDefineButtonCXform(
    Element parentElement, DefineButtonCXform tag) {
    Element element = parentElement.addElement("definebuttoncxform");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    RecordXMLWriter.writeCXForm(element, tag.getColorTransform());
  }

  private static void writeDefineButtonSound(
    Element parentElement, DefineButtonSound tag) {
    Element element = parentElement.addElement("definebuttonsound");
    element.addAttribute("buttonid", Integer.toString(tag.getButtonId()));
    if (tag.getOverUpToIdleSoundId() != 0) {
      Element overUpToIdle = element.addElement("overuptoidle");
      overUpToIdle.addAttribute(
        "soundid", Integer.toString(tag.getOverUpToIdleSoundId()));
      RecordXMLWriter.writeSoundInfo(
        overUpToIdle, tag.getOverUpToIdleSoundInfo());
    }
    if (tag.getIdleToOverUpSoundId() != 0) {
      Element idleToOverUp = element.addElement("idletooverup");
      idleToOverUp.addAttribute(
        "soundid", Integer.toString(tag.getIdleToOverUpSoundId()));
      RecordXMLWriter.writeSoundInfo(
        idleToOverUp, tag.getIdleToOverUpSoundInfo());
    }
    if (tag.getOverUpToOverDownSoundId() != 0) {
      Element overUpToOverDown = element.addElement("overuptooverdown");
      overUpToOverDown.addAttribute(
        "soundid", Integer.toString(tag.getOverUpToOverDownSoundId()));
      RecordXMLWriter.writeSoundInfo(
        overUpToOverDown, tag.getOverUpToOverDownSoundInfo());
    }
    if (tag.getOverDownToOverUpSoundId() != 0) {
      Element overDownToOverUp = element.addElement("overdowntooverup");
      overDownToOverUp.addAttribute(
        "soundid", Integer.toString(tag.getOverDownToOverUpSoundId()));
      RecordXMLWriter.writeSoundInfo(
        overDownToOverUp, tag.getOverDownToOverUpSoundInfo());
    }
  }

  private static void writeDefineEditText(
    Element parentElement, DefineEditText tag) {
    Element element = parentElement.addElement("defineedittext");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    if (tag.isWordWrap()) {
      element.addAttribute("wordwrap", "true");
    }
    if (tag.isMultiline()) {
      element.addAttribute("multiline", "true");
    }
    if (tag.isPassword()) {
      element.addAttribute("password", "true");
    }
    if (tag.isReadOnly()) {
      element.addAttribute("readonly", "true");
    }
    if (tag.isAutoSize()) {
      element.addAttribute("autosize", "true");
    }
    if (tag.isNoSelect()) {
      element.addAttribute("noselect", "true");
    }
    if (tag.isBorder()) {
      element.addAttribute("border", "true");
    }
    if (tag.isHtml()) {
      element.addAttribute("html", "true");
    }
    if (tag.isUseOutlines()) {
      element.addAttribute("useoutlines", "true");
    }
    if (tag.hasMaxLength()) {
      element.addAttribute("maxlength", Integer.toString(tag.getMaxLength()));
    }
    String var = tag.getVariableName();
    if ((var != null) && (var.length() > 0)) {
      element.addAttribute("variable", var);
    }
    RecordXMLWriter.writeRect(element, "bounds", tag.getBounds());
    if (tag.hasText()) {
      element.addElement("initialtext").addText(tag.getInitialText());
    }
    if (tag.hasTextColor()) {
      RecordXMLWriter.writeRGBA(element, "color", tag.getTextColor());
    }
    if (tag.hasFont()) {
      Element font = element.addElement("font");
      font.addAttribute("fontid", Integer.toString(tag.getFontId()));
      font.addAttribute("height", Integer.toString(tag.getFontHeight()));
    }
    if (tag.hasLayout()) {
      Element layout = element.addElement("layout");
      String align;
      switch (tag.getAlign()) {
        case DefineEditText.ALIGN_LEFT:
          align = "left";
          break;
        case DefineEditText.ALIGN_CENTER:
          align = "center";
          break;
        case DefineEditText.ALIGN_JUSTIFY:
          align = "justify";
          break;
        case DefineEditText.ALIGN_RIGHT:
          align = "right";
          break;
        default:
          throw new IllegalArgumentException(
            "Illegal text alignment: " + tag.getAlign());
      }
      layout.addAttribute("align", align);
      layout.addAttribute("leftmargin", Integer.toString(tag.getLeftMargin()));
      layout.addAttribute(
        "rightmargin", Integer.toString(tag.getRightMargin()));
      layout.addAttribute("indent", Integer.toString(tag.getIndent()));
      layout.addAttribute("leading", Integer.toString(tag.getLeading()));
    }
  }

  private static void writeDefineFont(Element parentElement, DefineFont tag) {
    Element element = parentElement.addElement("definefont");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    Element glyphShapeTableElement = element.addElement("glyphshapetable");
    Shape[] glyphShapeTable        = tag.getGlyphShapeTable();
    for (int i = 0; i < glyphShapeTable.length; i++) {
      RecordXMLWriter.writeShape(glyphShapeTableElement, glyphShapeTable[i]);
    }
  }

  private static void writeDefineFont2(Element parentElement, DefineFont2 tag) {
    Element element = parentElement.addElement("definefont2");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    element.addAttribute("fontname", tag.getFontName());
    if (tag.isANSI()) {
      element.addAttribute("ansi", "true");
    } else if (tag.isShiftJIS()) {
      element.addAttribute("shiftjis", "true");
    }
    if (tag.isBold()) {
      element.addAttribute("bold", "true");
    }
    if (tag.isItalic()) {
      element.addAttribute("italic", "true");
    }
    if (tag.isSmallText()) {
      element.addAttribute("smalltext", "true");
    }
    writeLanguage(element, tag.getLanguageCode());
    Shape[] table = tag.getGlyphShapeTable();
    if (table != null) {
      Element glyphShapeTableElement = element.addElement("glyphshapetable");
      char[] codeTable               = tag.getCodeTable();
      for (int i = 0; i < table.length; i++) {
        Element glyphElement = glyphShapeTableElement.addElement("glyph");
        glyphElement.addAttribute("char", Character.toString(codeTable[i]));
        RecordXMLWriter.writeShape(glyphElement, table[i]);
        if (tag.hasLayout()) {
          glyphElement.addAttribute(
            "advance", Short.toString(tag.getAdvanceTable()[i]));
          RecordXMLWriter.writeRect(
            glyphElement, "bounds", tag.getBoundsTable()[i]);
        }
      }
    }
    if (tag.hasLayout()) {
      Element layout = element.addElement("layout");
      layout.addAttribute("ascent", Integer.toString(tag.getAscent()));
      layout.addAttribute("descent", Integer.toString(tag.getDescent()));
      layout.addAttribute("leading", Integer.toString(tag.getLeading()));
      KerningRecord[] kerningTable = tag.getKerningTable();
      if ((kerningTable != null) && (kerningTable.length > 0)) {
        Element kerningTableElement = layout.addElement("kerningtable");
        for (int i = 0; i < kerningTable.length; i++) {
          KerningRecord record  = kerningTable[i];
          Element recordElement = kerningTableElement.addElement(
              "kerningrecord");
          recordElement.addAttribute(
            "left", Character.toString(record.getLeft()));
          recordElement.addAttribute(
            "right", Character.toString(record.getRight()));
          recordElement.addAttribute(
            "adjust", Short.toString(record.getAdjustment()));
        }
      }
    }
  }

  private static void writeDefineFont3(Element parentElement, DefineFont3 tag) {
    Element element = parentElement.addElement("definefont3");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    element.addAttribute("fontname", tag.getFontName());
    if (tag.isBold()) {
      element.addAttribute("bold", "true");
    }
    if (tag.isItalic()) {
      element.addAttribute("italic", "true");
    }
    if (tag.isSmallText()) {
      element.addAttribute("smalltext", "true");
    }
    writeLanguage(element, tag.getLanguageCode());
    Shape[] table = tag.getGlyphShapeTable();
    if (table != null) {
      Element glyphShapeTableElement = element.addElement("glyphshapetable");
      char[] codeTable               = tag.getCodeTable();
      for (int i = 0; i < table.length; i++) {
        Element glyphElement = glyphShapeTableElement.addElement("glyph");
        glyphElement.addAttribute("char", Character.toString(codeTable[i]));
        RecordXMLWriter.writeShape(glyphElement, table[i]);
        if (tag.hasLayout()) {
          glyphElement.addAttribute(
            "advance", Short.toString(tag.getAdvanceTable()[i]));
          RecordXMLWriter.writeRect(
            glyphElement, "bounds", tag.getBoundsTable()[i]);
        }
      }
    }
    if (tag.hasLayout()) {
      Element layout = element.addElement("layout");
      layout.addAttribute("ascent", Integer.toString(tag.getAscent()));
      layout.addAttribute("descent", Integer.toString(tag.getDescent()));
      layout.addAttribute("leading", Integer.toString(tag.getLeading()));
      KerningRecord[] kerningTable = tag.getKerningTable();
      if ((kerningTable != null) && (kerningTable.length > 0)) {
        Element kerningTableElement = layout.addElement("kerningtable");
        for (int i = 0; i < kerningTable.length; i++) {
          KerningRecord record  = kerningTable[i];
          Element recordElement = kerningTableElement.addElement(
              "kerningrecord");
          recordElement.addAttribute(
            "left", Character.toString(record.getLeft()));
          recordElement.addAttribute(
            "right", Character.toString(record.getRight()));
          recordElement.addAttribute(
            "adjust", Short.toString(record.getAdjustment()));
        }
      }
    }
  }

  private static void writeDefineFontAlignment(
    Element parentElement, DefineFontAlignment tag) {
    Element element = parentElement.addElement("definefontalignment");
    element.addAttribute("fontid", Integer.toString(tag.getFontId()));
    switch (tag.getThickness()) {
      case DefineFontAlignment.THIN:
        element.addAttribute("thickness", "thin");
        break;
      case DefineFontAlignment.MEDIUM:
        element.addAttribute("thickness", "medium");
        break;
      case DefineFontAlignment.THICK:
        element.addAttribute("thickness", "thick");
        break;
    }
    RecordXMLWriter.writeAlignmentZones(element, tag.getAlignmentZones());
  }

  private static void writeDefineFontInfo(
    Element parentElement, DefineFontInfo tag) {
    Element element = parentElement.addElement("definefontinfo");
    element.addAttribute("fontid", Integer.toString(tag.getFontId()));
    element.addAttribute("fontname", tag.getFontName());
    if (tag.isANSI()) {
      element.addAttribute("ansi", "true");
    } else if (tag.isShiftJIS()) {
      element.addAttribute("shiftjis", "true");
    }
    if (tag.isBold()) {
      element.addAttribute("bold", "true");
    }
    if (tag.isItalic()) {
      element.addAttribute("italic", "true");
    }
    if (tag.isSmallText()) {
      element.addAttribute("smalltext", "true");
    }
    char[] codeTable = tag.getCodeTable();
    for (int i = 0; i < codeTable.length; i++) {
      element.addElement("char").addText(Character.toString(codeTable[i]));
    }
  }

  private static void writeDefineFontInfo2(
    Element parentElement, DefineFontInfo2 tag) {
    Element element = parentElement.addElement("definefontinfo2");
    element.addAttribute("fontid", Integer.toString(tag.getFontId()));
    element.addAttribute("fontname", tag.getFontName());
    if (tag.isBold()) {
      element.addAttribute("bold", "true");
    }
    if (tag.isItalic()) {
      element.addAttribute("italic", "true");
    }
    if (tag.isSmallText()) {
      element.addAttribute("smalltext", "true");
    }
    writeLanguage(element, tag.getLangCode());
    char[] codeTable = tag.getCodeTable();
    for (int i = 0; i < codeTable.length; i++) {
      element.addElement("char").addText(Character.toString(codeTable[i]));
    }
  }

  private static void writeDefineMorphShape(
    Element parentElement, DefineMorphShape tag) {
    Element element = parentElement.addElement("definemorphshape");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    Shape startShape                = tag.getStartShape();
    Shape endShape                  = tag.getEndShape();
    MorphLineStyles morphLineStyles = tag.getMorphLineStyles();
    MorphFillStyles morphFillStyles = tag.getMorphFillStyles();
    boolean zeroOffset              = ((startShape == null) ||
      (endShape == null) || (morphLineStyles == null) ||
      (morphFillStyles == null));
    Element startElement            = element.addElement("start");
    RecordXMLWriter.writeRect(startElement, "bounds", tag.getStartBounds());
    Element endElement = element.addElement("end");
    RecordXMLWriter.writeRect(endElement, "bounds", tag.getEndBounds());
    if (!zeroOffset) {
      RecordXMLWriter.writeShape(startElement, startShape);
      RecordXMLWriter.writeShape(endElement, endShape);
      RecordXMLWriter.writeMorphLineStyles(element, morphLineStyles);
      RecordXMLWriter.writeMorphFillStyles(element, morphFillStyles);
    }
  }

  private static void writeDefineMorphShape2(
    Element parentElement, DefineMorphShape2 tag) {
    Element element = parentElement.addElement("definemorphshape2");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    Shape startShape                = tag.getStartShape();
    Shape endShape                  = tag.getEndShape();
    MorphLineStyles morphLineStyles = tag.getMorphLineStyles();
    MorphFillStyles morphFillStyles = tag.getMorphFillStyles();
    boolean zeroOffset              = ((startShape == null) ||
      (endShape == null) || (morphLineStyles == null) ||
      (morphFillStyles == null));
    Element startElement            = element.addElement("start");
    RecordXMLWriter.writeRect(
      startElement, "shapebounds", tag.getStartShapeBounds());
    RecordXMLWriter.writeRect(
      startElement, "edgebounds", tag.getStartEdgeBounds());
    Element endElement = element.addElement("end");
    RecordXMLWriter.writeRect(
      endElement, "shapebounds", tag.getEndShapeBounds());
    RecordXMLWriter.writeRect(endElement, "edgebounds", tag.getEndEdgeBounds());
    if (!zeroOffset) {
      RecordXMLWriter.writeShape(startElement, startShape);
      RecordXMLWriter.writeShape(endElement, endShape);
      RecordXMLWriter.writeMorphLineStyles(element, morphLineStyles);
      RecordXMLWriter.writeMorphFillStyles(element, morphFillStyles);
    }
  }

  private static void writeDefineShape(Element parentElement, DefineShape tag) {
    Element element = parentElement.addElement("defineshape");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    RecordXMLWriter.writeRect(element, "bounds", tag.getShapeBounds());
    RecordXMLWriter.writeShapeWithStyle(element, tag.getShapes());
  }

  private static void writeDefineShape2(
    Element parentElement, DefineShape2 tag) {
    Element element = parentElement.addElement("defineshape2");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    RecordXMLWriter.writeRect(element, "bounds", tag.getShapeBounds());
    RecordXMLWriter.writeShapeWithStyle(element, tag.getShapes());
  }

  private static void writeDefineShape3(
    Element parentElement, DefineShape3 tag) {
    Element element = parentElement.addElement("defineshape3");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    RecordXMLWriter.writeRect(element, "bounds", tag.getShapeBounds());
    RecordXMLWriter.writeShapeWithStyle(element, tag.getShapes());
  }

  private static void writeDefineShape4(
    Element parentElement, DefineShape4 tag) {
    Element element = parentElement.addElement("defineshape4");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    RecordXMLWriter.writeRect(element, "shapebounds", tag.getShapeBounds());
    RecordXMLWriter.writeRect(element, "edgebounds", tag.getEdgeBounds());
    RecordXMLWriter.writeShapeWithStyle(element, tag.getShapes());
  }

  private static void writeDefineSound(Element parentElement, DefineSound tag) {
    Element element = parentElement.addElement("definesound");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    element.addAttribute("format", getSoundFormatString(tag.getFormat()));
    element.addAttribute("rate", getSoundRateString(tag.getRate()));
    if (tag.is16BitSample()) {
      element.addAttribute("sample16bit", "true");
    }
    if (tag.isStereo()) {
      element.addAttribute("stereo", "true");
    }
    element.addAttribute("samplecount", Long.toString(tag.getSampleCount()));
    element.addText(Base64.encode(tag.getSoundData()));
  }

  private static void writeDefineSprite(
    Element parentElement, DefineSprite tag) {
    Element element = parentElement.addElement("definesprite");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    for (Iterator it = tag.getControlTags().iterator(); it.hasNext();) {
      Tag controlTag = (Tag) it.next();
      writeTag(element, controlTag);
    }
  }

  private static void writeDefineText(Element parentElement, DefineText tag) {
    Element element = parentElement.addElement("definetext");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    RecordXMLWriter.writeRect(element, "bounds", tag.getTextBounds());
    RecordXMLWriter.writeMatrix(element, "matrix", tag.getTextMatrix());
    Element textRecordsElement = element.addElement("textrecords");
    TextRecord[] textRecords   = tag.getTextRecords();
    for (int i = 0; i < textRecords.length; i++) {
      RecordXMLWriter.writeTextRecord(textRecordsElement, textRecords[i]);
    }
  }

  private static void writeDefineText2(Element parentElement, DefineText2 tag) {
    Element element = parentElement.addElement("definetext2");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    RecordXMLWriter.writeRect(element, "bounds", tag.getTextBounds());
    RecordXMLWriter.writeMatrix(element, "matrix", tag.getTextMatrix());
    Element textRecordsElement = element.addElement("textrecords");
    TextRecord[] textRecords   = tag.getTextRecords();
    for (int i = 0; i < textRecords.length; i++) {
      RecordXMLWriter.writeTextRecord(textRecordsElement, textRecords[i]);
    }
  }

  private static void writeDefineVideoStream(
    Element parentElement, DefineVideoStream tag) {
    Element element = parentElement.addElement("definevideostream");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    element.addAttribute("numframes", Integer.toString(tag.getNumFrames()));
    element.addAttribute("width", Integer.toString(tag.getWidth()));
    element.addAttribute("height", Integer.toString(tag.getHeight()));
    switch (tag.getDeblocking()) {
      case DefineVideoStream.DEBLOCKING_OFF:
        element.addAttribute("deblocking", "off");
        break;
      case DefineVideoStream.DEBLOCKING_ON:
        element.addAttribute("deblocking", "on");
        break;
      case DefineVideoStream.DEBLOCKING_PACKET:
        element.addAttribute("deblocking", "packet");
        break;
      default:
        throw new IllegalArgumentException(
          "Illegal video deblocking setting: " + tag.getDeblocking());
    }
    if (tag.isSmoothing()) {
      element.addAttribute("smoothing", "true");
    }
    switch (tag.getCodecId()) {
      case DefineVideoStream.CODEC_SCREEN_VIDEO:
        element.addAttribute("codec", "screenvideo");
        break;
      case DefineVideoStream.CODEC_SORENSON_H263:
        element.addAttribute("codec", "h263");
        break;
      case DefineVideoStream.CODEC_VP6:
        element.addAttribute("codec", "vp6");
        break;
      case DefineVideoStream.CODEC_VP6_ALPHA:
        element.addAttribute("codec", "vp6alpha");
        break;
      case DefineVideoStream.CODEC_SCREEN_VIDEO_V2:
        element.addAttribute("codec", "screenvideov2");
        break;
      case DefineVideoStream.CODEC_UNDEFINED:
        element.addAttribute("codec", "undefined");
        break;
      default:
        throw new IllegalArgumentException(
          "Illegal video codec ID: " + tag.getCodecId());
    }
  }

  private static void writeDoAction(Element parentElement, DoAction tag) {
    Element element = parentElement.addElement("doaction");
    RecordXMLWriter.writeActionBlock(element, tag.getActions());
  }

  private static void writeDoInitAction(
    Element parentElement, DoInitAction tag) {
    Element element = parentElement.addElement("doinitaction");
    element.addAttribute("spriteid", Integer.toString(tag.getSpriteId()));
    RecordXMLWriter.writeActionBlock(element, tag.getInitActions());
  }

  private static void writeEnableDebugger(
    Element parentElement, EnableDebugger tag) {
    Element element = parentElement.addElement("enabledebugger");
    String password = tag.getPassword();
    if (password != null) {
      element.addAttribute("password", password);
    }
  }

  private static void writeEnableDebugger2(
    Element parentElement, EnableDebugger2 tag) {
    Element element = parentElement.addElement("enabledebugger2");
    String password = tag.getPassword();
    if (password != null) {
      element.addAttribute("password", password);
    }
  }

  private static void writeExportAssets(
    Element parentElement, ExportAssets tag) {
    Element element                       = parentElement.addElement(
        "exportassets");
    ExportAssets.ExportMapping[] mappings = tag.getExportMappings();
    for (int i = 0; i < mappings.length; i++) {
      ExportAssets.ExportMapping mapping = mappings[i];
      Element mappingElement             = element.addElement("exportmapping");
      mappingElement.addAttribute(
        "charid", Integer.toString(mapping.getCharacterId()));
      mappingElement.addAttribute("name", mapping.getName());
    }
  }

  private static void writeFlashTypeSettings(
    Element parentElement, FlashTypeSettings tag) {
    Element element = parentElement.addElement("flashtypesettings");
    element.addAttribute("textid", Integer.toString(tag.getTextId()));
    element.addAttribute("flashtype", Boolean.toString(tag.isFlashType()));
    switch (tag.getGridFit()) {
      case FlashTypeSettings.GRID_FIT_NONE:
        element.addAttribute("gridfit", "none");
        break;
      case FlashTypeSettings.GRID_FIT_PIXEL:
        element.addAttribute("gridfit", "pixel");
        break;
      case FlashTypeSettings.GRID_FIT_SUBPIXEL:
        element.addAttribute("gridfit", "subpixel");
        break;
    }
    element.addAttribute(
      "thickness", StringUtilities.doubleToString(tag.getThickness()));
    element.addAttribute(
      "sharpness", StringUtilities.doubleToString(tag.getSharpness()));
  }

  private static void writeFrameLabel(Element parentElement, FrameLabel tag) {
    Element element = parentElement.addElement("framelabel");
    element.addAttribute("name", tag.getName());
    if (tag.isNamedAnchor()) {
      element.addAttribute("namedanchor", "true");
    }
  }

  private static void writeFreeCharacter(
    Element parentElement, FreeCharacter tag) {
    Element element = parentElement.addElement("freecharacter");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
  }

  private static void writeImportAssets(
    Element parentElement, ImportAssets tag) {
    Element element = parentElement.addElement("importassets");
    element.addAttribute("url", tag.getUrl());
    ImportAssets.ImportMapping[] mappings = tag.getImportMappings();
    for (int i = 0; i < mappings.length; i++) {
      ImportAssets.ImportMapping mapping = mappings[i];
      Element mappingElement             = element.addElement("importmapping");
      mappingElement.addAttribute("name", mapping.getName());
      mappingElement.addAttribute(
        "charid", Integer.toString(mapping.getCharacterId()));
    }
  }

  private static void writeImportAssets2(
    Element parentElement, ImportAssets2 tag) {
    Element element = parentElement.addElement("importassets2");
    element.addAttribute("url", tag.getUrl());
    ImportAssets.ImportMapping[] mappings = tag.getImportMappings();
    for (int i = 0; i < mappings.length; i++) {
      ImportAssets.ImportMapping mapping = mappings[i];
      Element mappingElement             = element.addElement("importmapping");
      mappingElement.addAttribute("name", mapping.getName());
      mappingElement.addAttribute(
        "charid", Integer.toString(mapping.getCharacterId()));
    }
  }

  private static void writeJPEGTables(Element parentElement, JPEGTables tag) {
    Element element = parentElement.addElement("jpegtables");
    element.addElement("jpegdata").addText(Base64.encode(tag.getJpegData()));
  }

  private static void writeLanguage(Element parentElement, LangCode langCode) {
    if (langCode != null) {
      switch (langCode.getLanguageCode()) {
        case LangCode.LATIN:
          parentElement.addAttribute("language", "latin");
          break;
        case LangCode.UNDEFINED:
          parentElement.addAttribute("language", "undefined");
          break;
        case LangCode.JAPANESE:
          parentElement.addAttribute("language", "japanese");
          break;
        case LangCode.KOREAN:
          parentElement.addAttribute("language", "korean");
          break;
        case LangCode.SIMPLIFIED_CHINESE:
          parentElement.addAttribute("language", "simpchinese");
          break;
        case LangCode.TRADITIONAL_CHINESE:
          parentElement.addAttribute("language", "tradchinese");
          break;
        default:
          throw new IllegalArgumentException(
            "Illegal language code: " + langCode.getLanguageCode());
      }
    }
  }

  private static void writeMalformedTag(
    Element parentElement, MalformedTag tag) {
    Element element = parentElement.addElement("malformedtag");
    element.addAttribute("code", Integer.toString(tag.getCode()));
    Exception exception      = tag.getException();
    Element exceptionElement = element.addElement("exception");
    exceptionElement.addAttribute("class", exception.getClass().getName());
    exceptionElement.addAttribute("message", exception.toString());
    element.addElement("data").addText(Base64.encode(tag.getData()));
  }

  private static void writePlaceObject(Element parentElement, PlaceObject tag) {
    Element element = parentElement.addElement("placeobject");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    element.addAttribute("depth", Integer.toString(tag.getDepth()));
    RecordXMLWriter.writeMatrix(element, tag.getMatrix());
    CXform colorTransform = tag.getColorTransform();
    if (colorTransform != null) {
      RecordXMLWriter.writeCXForm(element, colorTransform);
    }
  }

  private static void writePlaceObject2(
    Element parentElement, PlaceObject2 tag) {
    Element element = parentElement.addElement("placeobject2");
    element.addAttribute("depth", Integer.toString(tag.getDepth()));
    if (tag.hasCharacter()) {
      element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    }
    if (tag.hasName()) {
      element.addAttribute("name", tag.getName());
    }
    if (tag.isMove()) {
      element.addAttribute("move", "true");
    }
    if (tag.hasMatrix()) {
      RecordXMLWriter.writeMatrix(element, tag.getMatrix());
    }
    if (tag.hasColorTransform()) {
      RecordXMLWriter.writeCXFormWithAlpha(element, tag.getColorTransform());
    }
    if (tag.hasRatio()) {
      element.addAttribute("ratio", Integer.toString(tag.getRatio()));
    }
    if (tag.hasClipDepth()) {
      element.addAttribute("clipdepth", Integer.toString(tag.getClipDepth()));
    }
    if (tag.hasClipActions()) {
      RecordXMLWriter.writeClipActions(element, tag.getClipActions());
    }
  }

  private static void writePlaceObject3(
    Element parentElement, PlaceObject3 tag) {
    Element element = parentElement.addElement("placeobject3");
    element.addAttribute("depth", Integer.toString(tag.getDepth()));
    if (tag.hasCharacter()) {
      element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    }
    if (tag.hasName()) {
      element.addAttribute("name", tag.getName());
    }
    if (tag.isMove()) {
      element.addAttribute("move", "true");
    }
    if (tag.hasMatrix()) {
      RecordXMLWriter.writeMatrix(element, tag.getMatrix());
    }
    if (tag.hasColorTransform()) {
      RecordXMLWriter.writeCXFormWithAlpha(element, tag.getColorTransform());
    }
    if (tag.hasRatio()) {
      element.addAttribute("ratio", Integer.toString(tag.getRatio()));
    }
    if (tag.hasClipDepth()) {
      element.addAttribute("clipdepth", Integer.toString(tag.getClipDepth()));
    }
    if (tag.hasClipActions()) {
      RecordXMLWriter.writeClipActions(element, tag.getClipActions());
    }
    if (tag.hasBlendMode()) {
      element.addAttribute(
        "blendmode", BlendMode.getDescription(tag.getBlendMode()));
    }
    if (tag.isCacheAsBitmap()) {
      element.addAttribute("cacheasbitmap", "true");
    }
    if (tag.hasFilters()) {
      RecordXMLWriter.writeFilters(element, tag.getFilters());
    }
  }

  private static void writeProtect(Element parentElement, Protect tag) {
    Element element = parentElement.addElement("protect");
    String password = tag.getPassword();
    if (password != null) {
      element.addElement("password", tag.getPassword());
    }
  }

  private static void writeRemoveObject(
    Element parentElement, RemoveObject tag) {
    Element element = parentElement.addElement("removeobject");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    element.addAttribute("depth", Integer.toString(tag.getDepth()));
  }

  private static void writeRemoveObject2(
    Element parentElement, RemoveObject2 tag) {
    Element element = parentElement.addElement("removeobject2");
    element.addAttribute("depth", Integer.toString(tag.getDepth()));
  }

  private static void writeScale9Grid(Element parentElement, Scale9Grid tag) {
    Element element = parentElement.addElement("scale9grid");
    element.addAttribute("charid", Integer.toString(tag.getCharacterId()));
    RecordXMLWriter.writeRect(element, "grid", tag.getGrid());
  }

  private static void writeScriptLimits(
    Element parentElement, ScriptLimits tag) {
    Element element = parentElement.addElement("scriptlimits");
    element.addAttribute(
      "maxrecursiondepth", Integer.toString(tag.getMaxRecursionDepth()));
    element.addAttribute(
      "scripttimeout", Integer.toString(tag.getScriptTimeoutSeconds()));
  }

  private static void writeSetTabIndex(Element parentElement, SetTabIndex tag) {
    Element element = parentElement.addElement("settabindex");
    element.addAttribute("depth", Integer.toString(tag.getDepth()));
    element.addAttribute("tabindex", Integer.toString(tag.getTabIndex()));
  }

  private static void writeShowFrame(Element parentElement, ShowFrame tag) {
    parentElement.addElement("showframe");
  }

  private static void writeSoundStreamBlock(
    Element parentElement, SoundStreamBlock tag) {
    Element element = parentElement.addElement("soundstreamblock");
    element.addElement("streamsounddata").addText(
      Base64.encode(tag.getStreamSoundData()));
  }

  private static void writeSoundStreamHead(
    Element parentElement, SoundStreamHead tag) {
    Element element = parentElement.addElement("soundstreamhead");
    element.addAttribute(
      "streamformat", getSoundFormatString(tag.getStreamFormat()));
    element.addAttribute("streamrate", getSoundRateString(tag.getStreamRate()));
    if (tag.isStreamStereo()) {
      element.addAttribute("streamstereo", "true");
    }
    element.addAttribute(
      "streamsamplecount", Integer.toString(tag.getStreamSampleCount()));
    element.addAttribute(
      "playbackrate", getSoundRateString(tag.getPlaybackRate()));
    if (tag.isPlaybackStereo()) {
      element.addAttribute("playbackstereo", "true");
    }
    if (tag.getStreamFormat() == SoundStreamHead.FORMAT_MP3) {
      element.addAttribute("latencyseek", Short.toString(tag.getLatencySeek()));
    }
  }

  private static void writeSoundStreamHead2(
    Element parentElement, SoundStreamHead2 tag) {
    Element element = parentElement.addElement("soundstreamhead2");
    element.addAttribute(
      "streamformat", getSoundFormatString(tag.getStreamFormat()));
    element.addAttribute("streamrate", getSoundRateString(tag.getStreamRate()));
    if (tag.isStream16BitSample()) {
      element.addAttribute("streamsample16bit", "true");
    }
    if (tag.isStreamStereo()) {
      element.addAttribute("streamstereo", "true");
    }
    element.addAttribute(
      "streamsamplecount", Integer.toString(tag.getStreamSampleCount()));
    element.addAttribute(
      "playbackrate", getSoundRateString(tag.getPlaybackRate()));
    if (tag.isPlayback16BitSample()) {
      element.addAttribute("playbacksample16bit", "true");
    }
    if (tag.isPlaybackStereo()) {
      element.addAttribute("playbackstereo", "true");
    }
    if (tag.getStreamFormat() == SoundStreamHead.FORMAT_MP3) {
      element.addAttribute("latencyseek", Short.toString(tag.getLatencySeek()));
    }
  }

  private static void writeStartSound(Element parentElement, StartSound tag) {
    Element element = parentElement.addElement("startsound");
    element.addAttribute("soundid", Integer.toString(tag.getSoundId()));
    RecordXMLWriter.writeSoundInfo(element, tag.getSoundInfo());
  }

  private static void writeUnknownTag(Element parentElement, UnknownTag tag) {
    Element element = parentElement.addElement("unknowntag");
    element.addAttribute("code", Integer.toString(tag.getCode()));
    element.addText(Base64.encode(tag.getData()));
  }

  private static void writeVideoFrame(Element parentElement, VideoFrame tag) {
    Element element = parentElement.addElement("videoframe");
    element.addAttribute("streamid", Integer.toString(tag.getStreamId()));
    element.addAttribute("framenum", Integer.toString(tag.getFrameNum()));
    element.addElement("videodata").addText(Base64.encode(tag.getVideoData()));
  }
}
