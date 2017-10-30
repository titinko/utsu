package com.utsusynth.utsu.common;

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;

/** Tests for the {@link PitchUtilsTest}. */
public class PitchUtilsTest {
  @Test
  public void toAndFromNoteNum() {
    testBothNoteNumConversions("C0", 12);
    testBothNoteNumConversions("C#0", 13);
    testBothNoteNumConversions("C1", 24);
    testBothNoteNumConversions("C#1", 25);
    testBothNoteNumConversions("F#4", 66);
    testBothNoteNumConversions("B7", 107);
  }

  private static void testBothNoteNumConversions(String pitchString, int noteNum) {
    assertWithMessage("Converting note num %s to pitch", noteNum)
        .that(PitchUtils.noteNumToPitch(noteNum))
        .isEqualTo(pitchString);
    assertWithMessage("Converting %s to num")
        .that(PitchUtils.pitchToNoteNum(pitchString))
        .isEqualTo(noteNum);
  }

  @Test
  public void toAndFromRowNum() {
    testBothRowNumConversions("B7", 0);
    testBothRowNumConversions("B6", 12);
    testBothRowNumConversions("A#6", 13);
    testBothRowNumConversions("B5", 24);
    testBothRowNumConversions("A#5", 25);
    testBothRowNumConversions("C0", 95);
  }

  private static void testBothRowNumConversions(String pitchString, int rowNum) {
    assertWithMessage("Converting row %s to pitch", rowNum)
        .that(PitchUtils.rowNumToPitch(rowNum))
        .isEqualTo(pitchString);
    assertWithMessage("Converting %s to row num")
        .that(PitchUtils.pitchToRowNum(pitchString))
        .isEqualTo(rowNum);
  }
}
