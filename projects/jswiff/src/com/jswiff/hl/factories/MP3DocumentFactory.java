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

package com.jswiff.hl.factories;

import com.jswiff.SWFDocument;
import com.jswiff.SWFWriter;
import com.jswiff.io.InputBitStream;
import com.jswiff.swfrecords.SoundInfo;
import com.jswiff.swfrecords.tags.DefineSound;
import com.jswiff.swfrecords.tags.ShowFrame;
import com.jswiff.swfrecords.tags.SoundStreamHead2;
import com.jswiff.swfrecords.tags.StartSound;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Creates a document from a mp3 sound file.
 *
 * @author <a href="mailto:ralf@terdic.de">Ralf Terdic</a>
 */
public class MP3DocumentFactory {
  private static final int[][] BIT_RATES       = {
      { 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0 }, // MPEG2.5
      { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, // reserved
      { 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0 }, // MPEG2
      { 0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0 } // MPEG1
    };
  private static final int[][] SAMPLING_RATES  = {
      {11025, 12000, 8000, 0}, // MPEG2.5
      {0, 0, 0, 0}, // reserved
      {22050, 24000, 16000, 0}, // MPEG2
      {44100, 48000, 32000, 0} // MPEG1
    };
  private static final int[] CHANNELS          = { 2, 2, 2, 1 };
  private static final int[] SAMPLES_PER_FRAME = { 576, 576, 576, 1152 };
  private byte[] mp3Data;
  private InputBitStream mp3BitStream;
  private byte[] soundData;
  private SWFDocument doc;
  private int sampleCount;
  private int channelCount;
  private int samplingRate;

  /**
   * Creates a new MP3DocumentFactory instance.
   *
   * @param mp3Stream mp3 stream
   *
   * @throws IOException if something went wrong while reading from the mp3
   *         stream
   */
  public MP3DocumentFactory(InputStream mp3Stream) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer              = new byte[1000];
    int bytesRead;
    while ((bytesRead = mp3Stream.read(buffer)) > 0) {
      baos.write(buffer, 0, bytesRead);
    }
    mp3Data = baos.toByteArray();
    mp3BitStream = new InputBitStream(mp3Data);
  }

  /**
   * Returns the created document.
   *
   * @return SWF doc
   *
   * @throws IOException if an error occured during parsing the mp3 stream
   */
  public SWFDocument getDocument() throws IOException {
    initDocument();
    return doc;
  }

  /**
   * Main method for quick tests, pass input file (mp3) and output file (swf).
   *
   * @param args mp3, swf files
   *
   * @throws IOException if an I/O error occured
   */
  public static void main(String[] args) throws IOException {
    String mp3FileName                 = args[0];
    String swfFileName                 = args[1];
    MP3DocumentFactory documentFactory = new MP3DocumentFactory(
        new FileInputStream(mp3FileName));
    SWFDocument document               = documentFactory.getDocument();
    SWFWriter writer                   = new SWFWriter(
        document, new FileOutputStream(swfFileName));
    writer.write();
  }

  private byte getSamplingRateCode() {
    switch (samplingRate) {
      case 11025:
        return 1;
      case 22050:
        return 2;
      case 44100:
        return 3;
      default:
        return 0;
    }
  }

  private void countSamples() throws IOException {
    sampleCount = 0;
    while (true) {
      int mpegVersion = (int) mp3BitStream.readUnsignedBits(2);
      /* int mpegLayer   = (int) */ mp3BitStream.readUnsignedBits(2);
      mp3BitStream.readBooleanBit(); // CRC
      int bitRate = BIT_RATES[mpegVersion][(int) mp3BitStream.readUnsignedBits(
          4)];
      samplingRate = SAMPLING_RATES[mpegVersion][(int) mp3BitStream.readUnsignedBits(
          2)];
      if ((bitRate == 0) || (samplingRate == 0)) {
        // skip frame
        if (findNextFrame()) {
          continue;
        }
        break;
      }
      int padding = (int) mp3BitStream.readUnsignedBits(1);
      mp3BitStream.readBooleanBit(); // reserved
      channelCount = CHANNELS[(int) mp3BitStream.readUnsignedBits(2)];
      mp3BitStream.readUnsignedBits(6); // ignore 6 bits
      sampleCount += SAMPLES_PER_FRAME[mpegVersion];
      int frameSize = (((((mpegVersion == 3) ? 144 : 72) * bitRate * 1000) / samplingRate) +
        padding) - 4;
      mp3BitStream.move(frameSize);
      if (!findNextFrame()) {
        break;
      }
    }
  }

  private boolean findNextFrame() {
    // find next frame sync, i.e. 11 set bits
    try {
      while (true) {
        if (mp3BitStream.readUI8() != 0xff) {
          continue;
        }
        long value = mp3BitStream.readUnsignedBits(3);
        if (value == 7) {
          return true;
        }
        mp3BitStream.align();
      }
    } catch (IOException e) {
      return false; // end reached
    }
  }

  private void initDocument() throws IOException {
    doc = new SWFDocument();
    doc.setCompressed(true);
    initSoundData();
    countSamples();
    doc.addTag(
      new SoundStreamHead2(
        SoundStreamHead2.FORMAT_MP3, getSamplingRateCode(), true,
        (channelCount == 2), 0));
    int soundId = doc.getNewCharacterId();
    doc.addTag(
      new DefineSound(
        soundId, DefineSound.FORMAT_MP3, getSamplingRateCode(), true,
        (channelCount == 2), sampleCount, soundData));
    doc.addTag(new StartSound(soundId, new SoundInfo()));
    doc.addTag(new ShowFrame());
  }

  private void initSoundData() throws IOException {
    // strip off file header (ID tags etc.)
    if (!findNextFrame()) {
      throw new IOException("MP3 stream contains no frames!");
    }
    int offset = (int) (mp3BitStream.getOffset() - 2);
    soundData      = new byte[mp3Data.length - offset + 2];
    soundData[0]   = 0;
    soundData[1]   = 0;
    System.arraycopy(mp3Data, offset, soundData, 2, mp3Data.length - offset);
  }
}
