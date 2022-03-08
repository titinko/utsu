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
            NoteData note = noteContextData.getNote();
            String strippedLyric = stripLyric(note.getLyric(), voicebankData);
            Optional<String> strippedPrevLyric = noteContextData.getPrev().map(
                    prev -> stripLyric(prev.getLyric(), voicebankData));
            Optional<String> strippedNextLyric = noteContextData.getNext().map(
                    next -> stripLyric(next.getLyric(), voicebankData));

            // Remove note if it appears to be VC.
            if (looksLikeVc(
                    noteContextData,
                    strippedLyric,
                    strippedPrevLyric,
                    strippedNextLyric,
                    voicebankData)) {
                continue;
            }

            // Lengthen note if it appears to be followed by a normal VC.
            if (noteContextData.getNext().isPresent()
                    && strippedNextLyric.isPresent()
                    && followedByNonEndVc(
                            strippedLyric,
                            strippedNextLyric.get(),
                            voicebankData)) {
                int nextDuration = noteContextData.getNext().get().getDuration();
                output.add(note.withDuration(note.getDuration() + nextDuration));
                continue;
            }
            output.add(noteContextData.getNote());
        }
        return output.build();
    }

    private static boolean looksLikeVc(
            NoteContextData noteContext,
            String strippedLyric,
            Optional<String> strippedPrevLyric,
            Optional<String> strippedNextLyric,
            VoicebankData voicebankData) {
        if (strippedPrevLyric.isEmpty()) {
            return false; // The first note will not be VC.
        }
        Optional<String> prevVowel =
                LyricUtils.guessJpVowel(strippedPrevLyric.get(), voicebankData);
        if (noteContext.getNext().isEmpty() ||
                strippedNextLyric.isEmpty() ||
                (noteContext.getNote().getPosition() + noteContext.getNote().getDuration()
                    < noteContext.getNext().get().getPosition())) {
            // Check for an ending VC when there's no adjoining next note.
            Optional<String> endVc = guessEndVcLyric(voicebankData, prevVowel);
            return endVc.isPresent() && strippedLyric.equals(endVc.get());
        }
        Optional<String> nextConsonant =
                LyricUtils.guessJpConsonant(strippedNextLyric.get(), voicebankData);
        Optional<String> vcLyric = guessVcLyric(voicebankData, nextConsonant, prevVowel);
        return vcLyric.isPresent() && strippedLyric.equals(vcLyric.get());
    }

    private static boolean followedByNonEndVc(
            String strippedLyric, String strippedNextLyric, VoicebankData voicebankData) {
        Optional<String> vowel = LyricUtils.guessJpVowel(strippedLyric, voicebankData);
        Optional<String> vcEnd = guessEndVcLyric(voicebankData, vowel);
        Optional<String> partialVc = guessVcLyric(voicebankData, Optional.of(""), vowel);
        boolean followedByEndVc = vcEnd.isPresent() && strippedNextLyric.equals(vcEnd.get());
        boolean followedByVc =
                partialVc.isPresent() && strippedNextLyric.startsWith(partialVc.get());
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

    private static String stripLyric(String lyric, VoicebankData voicebankData) {
        String prefix = LyricUtils.guessJpPrefix(lyric, voicebankData);
        String suffix = LyricUtils.guessJpSuffix(lyric, voicebankData);
        return LyricUtils.stripPrefixSuffix(lyric, prefix, suffix);
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
