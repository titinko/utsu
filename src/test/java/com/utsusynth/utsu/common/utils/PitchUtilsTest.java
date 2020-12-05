package com.utsusynth.utsu.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Tests for {@link PitchUtils}. */
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
    assertEquals(
            PitchUtils.noteNumToPitch(noteNum),
            pitchString,
            "Converting note num " + noteNum + "to pitch"
    );
    assertEquals(
            PitchUtils.pitchToNoteNum(pitchString),
            noteNum,
            "Converting " + pitchString + " to num"
    );
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
    assertEquals(
            PitchUtils.rowNumToPitch(rowNum),
            pitchString,
            "Converting row " + rowNum + "to pitch"
    );
    assertEquals(
            PitchUtils.pitchToRowNum(pitchString),
            rowNum,
            "Converting " + pitchString + " to row num"
    );
  }
}
