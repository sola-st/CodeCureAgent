```java
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.joshua.packed;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import org.apache.joshua.util.quantization.Quantizer;
import org.apache.joshua.util.quantization.QuantizerConfiguration;
import org.apache.joshua.corpus.Vocabulary;

/**
 * This program reads a packed representation and prints out some basic
 * information about it.
 * 
 * Usage: java PrintRules PACKED_GRAMMAR_DIR
 */

public class PrintRules {

  private QuantizerConfiguration quantization;

  private int[] source;
  private int[] target;
  private MappedByteBuffer features;
  private MappedByteBuffer alignments;

  private int[] featureLookup;
  private int[] alignmentLookup;

  private boolean haveAlignments;

  public PrintRules(String dir) throws IOException {
    File sourceFile = new File(dir + "/slice_00000.source");
    File targetFile = new File(dir + "/slice_00000.target");
    File featureFile = new File(dir + "/slice_00000.features");
    File alignmentFile = new File(dir + "/slice_00000.alignments");

    haveAlignments = alignmentFile.exists();

    // Read the vocabulary.
    Vocabulary.read(new File(dir + "/vocabulary"));

    // Read the quantizer setup.
    quantization = new QuantizerConfiguration();
    quantization.read(dir + "/quantization");

    // Get the channels etc.
    @SuppressWarnings("resource")
    FileChannel sourceChannel = new FileInputStream(sourceFile).getChannel();
    int sourceSize = (int) sourceChannel.size();
    IntBuffer sourceBuffer = sourceChannel.map(MapMode.READ_ONLY, 0,
        sourceSize).asIntBuffer();
    source = new int[sourceSize / 4];
    sourceBuffer.get(source);

    @SuppressWarnings("resource")
    FileChannel targetChannel = new FileInputStream(targetFile).getChannel();
    int targetSize = (int) targetChannel.size();
    IntBuffer targetBuffer = targetChannel.map(MapMode.READ_ONLY, 0, 
        targetSize).asIntBuffer();
    target = new int[targetSize / 4];
    targetBuffer.get(target);

    @SuppressWarnings("resource")
    FileChannel featureChannel = new FileInputStream(featureFile).getChannel();
    int featureSize = (int) featureChannel.size();
    features = featureChannel.map(MapMode.READ_ONLY, 0, featureSize);

    if (haveAlignments) {
      @SuppressWarnings("resource")
      FileChannel alignmentChannel = new FileInputStream(alignmentFile).getChannel();
      int alignmentSize = (int) alignmentChannel.size();
      alignments = alignmentChannel.map(MapMode.READ_ONLY, 0, alignmentSize);
    }

    int numFeatureBlocks = features.getInt();
    featureLookup = new int[numFeatureBlocks];
    // Read away data size.
    features.getInt();
    for (int i = 0; i < numFeatureBlocks; i++)
      featureLookup[i] = features.getInt();

    int numAlignmentBlocks = alignments.getInt(); 
    alignmentLookup = new int[numAlignmentBlocks];
    // Read away data size.
    alignments.getInt();
    for (int i = 0; i < numAlignmentBlocks; i++)
      alignmentLookup[i] = alignments.getInt();

    if (numAlignmentBlocks != numFeatureBlocks)
      throw new RuntimeException("Number of blocks doesn't match up.");
  }

  public void traverse() {
    traverse(0, "");
  }

  private void traverse(int position, String srcSide) {
    int numChildren = source[position];
    int[] addresses = new int[numChildren];
    int[] symbols = new int[numChildren];
    int j = position + 1;
    for (int i = 0; i < numChildren; i++) {
      symbols[i] = source[j++];
      addresses[i] = source[j++];
    }
    int numRules = source[j++];
    for (int i = 0; i < numRules; i++) {
      int lhs = source[j++];
      int tgtAddress = source[j++];
      int dataAddress = source[j++];
      printRule(srcSide, lhs, tgtAddress, dataAddress);
    }
    for (int i = 0; i < numChildren; i++) {
      traverse(addresses[i], srcSide + " " + Vocabulary.word(symbols[i]));
    }
  }

  private String getTarget(int pointer) {
    StringBuilder sb = new StringBuilder();
    do {
      pointer = target[pointer];
      if (pointer != -1) {
        int symbol = target[pointer + 1];
        if (symbol < 0)
          sb.append(" ").append("NT" + symbol);
        else
          sb.append(" ").append(Vocabulary.word(symbol));
      }
    } while (pointer != -1);
    return sb.toString();
  }

  private String getFeatures(int blockId) {
    StringBuilder sb = new StringBuilder();

    int dataPosition = featureLookup[blockId];
    int numFeatures = features.getInt(dataPosition);
    dataPosition += 4;
    for (int i = 0; i < numFeatures; i++) {
      int featureId = features.getInt(dataPosition);
      Quantizer quantizer = quantization.get(featureId);
      sb.append(" " + Vocabulary.word(featureId) + "=" +
          quantizer.read(features, dataPosition));
      dataPosition += 4 + quantizer.size();
    }
    return sb.toString();
  }

  private String getAlignments(int blockId) {
    StringBuilder sb = new StringBuilder();

    int dataPosition = alignmentLookup[blockId];
    byte numPoints = alignments.get(dataPosition);
    for (int i = 0; i < numPoints; i++) {
      byte src = alignments.get(dataPosition + 1 + 2 * i);
      byte tgt = alignments.get(dataPosition + 2 + 2 * i);

      sb.append(" " + src + "-" + tgt);
    }
    return sb.toString();
  }

  private void printRule(String srcSide, int lhs, int tgtAddress,
      int dataAddress) {
    System.out.println(Vocabulary.word(lhs) + " |||" +
        srcSide + " |||" +
        getTarget(tgtAddress) + " |||" +
        getFeatures(dataAddress) + 
        (haveAlignments ? " |||" + getAlignments(dataAddress) : ""));
  }

  public static void main(String[] args) throws IOException {
    PrintRules pr = new PrintRules(args[0]);
    pr.traverse();
  }
}

