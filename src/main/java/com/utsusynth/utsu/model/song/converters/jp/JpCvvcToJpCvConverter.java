package com.utsusynth.utsu.model.song.converters.jp;

import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.data.NoteContextData;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.VoicebankData;
import com.utsusynth.utsu.common.enums.ReclistType;
import com.utsusynth.utsu.common.utils.LyricUtils;
import com.utsusynth.utsu.model.song.converters.ReclistConverter;
import com.utsusynth.utsu.model.voicebank.PresampConfig.AliasType;

import java.util.List;
import java.util.Optional;

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
                    && followedByNonEndVc(
                            note.getLyric(),
                            noteContextData.getNext().get().getLyric(),
                            voicebankData)) {
                int nextDuration = noteContextData.getNext().get().getDuration();
                output.add(note.withDuration(note.getDuration() + nextDuration));
                continue; // Lengthen note if it appears to be followed by a normal VC.
            }
            output.add(noteContextData.getNote());
        }
        return output.build();
    }

    private static boolean looksLikeVc(NoteContextData noteContext, VoicebankData voicebankData) {
        if (noteContext.getPrev().isEmpty()) {
            return false; // The first note will not be VC.
        }
        Optional<String> prevVowel = LyricUtils.guessJpVowel(
                noteContext.getPrev().get().getLyric(), voicebankData);
        if (noteContext.getNext().isEmpty() ||
                (noteContext.getNote().getPosition() + noteContext.getNote().getDuration()
                    < noteContext.getNext().get().getPosition())) {
            // Check for an ending VC when there's no adjoining next note.
            Optional<String> endVc = guessEndVcLyric(voicebankData, prevVowel);
            return endVc.isPresent() && noteContext.getNote().getLyric().equals(endVc.get());
        }
        Optional<String> nextConsonant = LyricUtils.guessJpConsonant(
                noteContext.getNext().get().getLyric(), voicebankData);
        Optional<String> vcLyric = guessVcLyric(voicebankData, nextConsonant, prevVowel);
        return vcLyric.isPresent() && noteContext.getNote().getLyric().equals(vcLyric.get());
    }

    private static boolean followedByNonEndVc(
            String lyric, String nextLyric, VoicebankData voicebankData) {
        Optional<String> vowel = LyricUtils.guessJpVowel(lyric, voicebankData);
        Optional<String> vcEnd = guessEndVcLyric(voicebankData, vowel);
        Optional<String> partialVc = guessVcLyric(voicebankData, Optional.of(""), vowel);
        boolean followedByEndVc = vcEnd.isPresent() && nextLyric.equals(vcEnd.get());
        boolean followedByVc = partialVc.isPresent() && nextLyric.startsWith(partialVc.get());
        return followedByVc && !followedByEndVc; // End VC can look like VC.
    }

    private static Optional<String> guessVcLyric(
            VoicebankData voicebankData,
            Optional<String> nextConsonant,
            Optional<String> prevVowel) {
        String endVc = voicebankData.getPresampConfig().parseAlias(
                AliasType.VC,
                "",
                nextConsonant,
                prevVowel,
                Optional.empty());
        return endVc.isEmpty() ? Optional.empty() : Optional.of(endVc);
    }

    private static Optional<String> guessEndVcLyric(
            VoicebankData voicebankData, Optional<String> prevVowel) {
        String endVc = voicebankData.getPresampConfig().parseAlias(
                AliasType.ENDING_1,
                "",
                Optional.empty(),
                prevVowel,
                Optional.empty());
        return endVc.isEmpty() ? Optional.empty() : Optional.of(endVc);
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
