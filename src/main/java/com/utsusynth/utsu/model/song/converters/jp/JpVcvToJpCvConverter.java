package com.utsusynth.utsu.model.song.converters.jp;

import com.utsusynth.utsu.common.data.NoteContextData;
import com.utsusynth.utsu.common.data.NoteData;
import com.utsusynth.utsu.common.data.VoicebankData;
import com.utsusynth.utsu.common.enums.ReclistType;
import com.utsusynth.utsu.common.utils.LyricUtils;
import com.utsusynth.utsu.model.song.converters.ReclistConverter;
import com.utsusynth.utsu.model.voicebank.PresampConfig.AliasType;

import java.util.List;
import java.util.stream.Collectors;

public class JpVcvToJpCvConverter implements ReclistConverter {
    @Override
    public List<NoteData> apply(List<NoteContextData> notes, VoicebankData voicebankData) {
        return notes.stream().map(noteContext -> {
            NoteData note = noteContext.getNote();
            // Get prefix, suffix, and stripped lyric.
            String prefix = LyricUtils.guessJpPrefix(note.getLyric(), voicebankData);
            String suffix = LyricUtils.guessJpSuffix(note.getLyric(), voicebankData);
            String strippedLyric = LyricUtils.stripPrefixSuffix(note.getLyric(), prefix, suffix);

            String vcvPad = voicebankData.getPresampConfig().parseAlias(AliasType.VCVPAD, " ");
            int index = strippedLyric.indexOf(vcvPad);
            if (index == -1) {
                return note;
            }
            String newStrippedLyric = strippedLyric.substring(index + vcvPad.length());
            if (newStrippedLyric.isEmpty()) {
                return note;
            }
            return note.withNewLyric(prefix + newStrippedLyric + suffix);
        }).collect(Collectors.toList());
    }

    @Override
    public ReclistType getFrom() {
        return ReclistType.JP_VCV;
    }

    @Override
    public ReclistType getTo() {
        return ReclistType.JP_CV;
    }
}
