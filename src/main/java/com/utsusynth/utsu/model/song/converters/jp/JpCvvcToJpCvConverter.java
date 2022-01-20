package com.utsusynth.utsu.model.song.converters.jp;

import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.data.NoteContextData;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.VoicebankData;
import com.utsusynth.utsu.common.enums.ReclistType;
import com.utsusynth.utsu.common.utils.LyricUtils;
import com.utsusynth.utsu.model.song.converters.ReclistConverter;

import java.util.List;

public class JpCvvcToJpCvConverter implements ReclistConverter {
    @Override
    public List<NoteData> apply(List<NoteContextData> notes, VoicebankData voicebankData) {
        ImmutableList.Builder<NoteData> output = ImmutableList.builder();
        for (NoteContextData noteContextData : notes) {
            if (looksLikeVc(noteContextData, voicebankData)) {
                continue; // Remove note if it appears to be VC.
            }
            NoteData note = noteContextData.getNote();
            if (noteContextData.getNext().isPresent()
                    && followedByVc(
                            note.getLyric(),
                            noteContextData.getNext().get().getLyric(),
                            voicebankData)) {
                int nextDuration = noteContextData.getNext().get().getDuration();
                output.add(note.withDuration(note.getDuration() + nextDuration));
                continue; // Lengthen note if it appears to be followed by a VC.
            }
            output.add(noteContextData.getNote());
        }
        return output.build();
    }

    private static boolean looksLikeVc(NoteContextData noteContext, VoicebankData voicebankData) {
        if (noteContext.getPrev().isEmpty()) {
            return false; // The first note will not be VC.
        }
        String prevVowel = LyricUtils.guessJpVowel(
                noteContext.getPrev().get().getLyric(), voicebankData.getLyricConversions());
        if (noteContext.getNext().isEmpty()) {
            return noteContext.getNote().getLyric().equals(prevVowel + " -");
        }
        String nextConsonant = LyricUtils.guessJpConsonant(
                noteContext.getNext().get().getLyric(), voicebankData.getLyricConversions());
        return noteContext.getNote().getLyric().equals(prevVowel + " " + nextConsonant);
    }

    private static boolean followedByVc(
            String lyric, String nextLyric, VoicebankData voicebankData) {
        String vowel = LyricUtils.guessJpVowel(lyric, voicebankData.getLyricConversions());
        String nextVowel = LyricUtils.guessJpVowel(nextLyric, voicebankData.getLyricConversions());
        return !(vowel.isEmpty()) && nextVowel.isEmpty() && nextLyric.startsWith(vowel + " ");
    }

    @Override
    public ReclistType getFrom() {
        return ReclistType.JP_CVVC;
    }

    @Override
    public ReclistType getTo() {
        return ReclistType.JP_CV;
    }
}
