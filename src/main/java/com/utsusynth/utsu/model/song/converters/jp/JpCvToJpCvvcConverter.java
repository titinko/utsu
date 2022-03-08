package com.utsusynth.utsu.model.song.converters.jp;

import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.data.NoteContextData;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.VoicebankData;
import com.utsusynth.utsu.common.enums.ReclistType;
import com.utsusynth.utsu.common.utils.LyricUtils;
import com.utsusynth.utsu.model.song.converters.ReclistConverter;
import com.utsusynth.utsu.model.voicebank.LyricConfig;
import com.utsusynth.utsu.model.voicebank.PresampConfig;

import java.util.List;
import java.util.Optional;

public class JpCvToJpCvvcConverter implements ReclistConverter {
    @Override
    public List<NoteData> apply(List<NoteContextData> notes, VoicebankData voicebankData) {
        ImmutableList.Builder<NoteData> output = ImmutableList.builder();
        for (NoteContextData noteContextData : notes) {
            NoteData note = noteContextData.getNote();
            Optional<NoteData> vcNote;
            if (noteContextData.getNext().isEmpty()) {
                vcNote = makeVcNote(note, "", voicebankData);
            } else {
                NoteData next = noteContextData.getNext().get();
                vcNote = makeVcNote(note, next.getLyric(), voicebankData);
            }
            if (vcNote.isPresent()) {
                output.add(note.withDuration(note.getDuration() - vcNote.get().getDuration()));
                output.add(vcNote.get());
            } else {
                output.add(note);
            }
        }
        return output.build();
    }

    private Optional<NoteData> makeVcNote(
            NoteData note, String nextLyric, VoicebankData voicebankData) {
        Optional<String> vcLyric = makeVcLyric(note.getLyric(), nextLyric, voicebankData);
        if (vcLyric.isEmpty()) {
            return Optional.empty();
        }
        int vcLength = guessVcLength(note, nextLyric, voicebankData);
        if (vcLength == 0) {
            return Optional.empty();
        }
        int vcStart = note.getPosition() + note.getDuration() - vcLength;
        return Optional.of(new NoteData(vcStart, vcLength, note.getPitch(), vcLyric.get()));
    }

    private static Optional<String> makeVcLyric(
            String prevLyric, String nextLyric, VoicebankData voicebankData) {
        String prevPrefix = LyricUtils.guessJpPrefix(prevLyric, voicebankData);
        String prevSuffix = LyricUtils.guessJpSuffix(prevLyric, voicebankData);
        String strippedPrevLyric = LyricUtils.stripPrefixSuffix(prevLyric, prevPrefix, prevSuffix);
        Optional<String> prevVowel = LyricUtils.guessJpVowel(strippedPrevLyric, voicebankData);

        String nextPrefix = LyricUtils.guessJpPrefix(nextLyric, voicebankData);
        String nextSuffix = LyricUtils.guessJpSuffix(nextLyric, voicebankData);
        String strippedNextLyric = LyricUtils.stripPrefixSuffix(nextLyric, nextPrefix, nextSuffix);
        Optional<String> nextConsonant =
                LyricUtils.guessJpConsonant(strippedNextLyric, voicebankData);

        String endVc = voicebankData.getPresampConfig().parseAlias(
                PresampConfig.AliasType.VC,
                "",
                nextConsonant,
                prevVowel,
                Optional.empty());
        return endVc.isEmpty() ? Optional.empty() : Optional.of(prevPrefix + endVc + prevSuffix);
    }

    private int guessVcLength(
            NoteData note, String nextLyric, VoicebankData voicebankData) {
        int max = note.getDuration() / 2;
        int min = 10;
        // Case where note is way too small.
        if (max < min) {
            return 0;
        }
        // First: try the preutterance of the next note.
        Optional<LyricConfig> nextNote = guessLyricConfig(nextLyric, note.getPitch(), voicebankData);
        if (nextNote.isPresent()) {
            return Math.min(max, Math.max(min, (int) nextNote.get().getPreutterance()));
        }
        // Second: try the length of entire oto of VC note.
        // Optional<LyricConfig> vcConfig = guessLyricConfig(vcLyric, note.getPitch(), voicebankData);
        // if (vcConfig.isPresent()) {
        //     return Math.min(max, Math.max(min, (int) vcConfig.get().getPreutterance()));
        //}
        // Third: try 80 ms.
        int defaultLength = 80;
        return Math.min(max, defaultLength);
    }

    private Optional<LyricConfig> guessLyricConfig(
            String lyric, String pitch, VoicebankData voicebankData) {
        String prefix = voicebankData.getPitchMap().getPrefix(pitch);
        String suffix = voicebankData.getPitchMap().getSuffix(pitch);
        String[] possibleLyrics =
                new String[] {lyric, lyric + suffix, prefix + lyric, prefix + lyric + suffix};
        for (String possibleLyric : possibleLyrics) {
            if (voicebankData.getLyricConfigs().getConfig(possibleLyric).isPresent()) {
                return voicebankData.getLyricConfigs().getConfig(possibleLyric);
            }
        }
        return Optional.empty();
    }

    @Override
    public ReclistType getFrom() {
        return ReclistType.JP_CV;
    }

    @Override
    public ReclistType getTo() {
        return ReclistType.JP_CVVC;
    }
}
