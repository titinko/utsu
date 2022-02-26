package com.utsusynth.utsu.common.utils;

import com.utsusynth.utsu.model.voicebank.PresampConfig.AliasType;

import java.util.Optional;
import java.util.regex.Pattern;

/* Common operations to perform using a presamp.ini common in several plugins. */
public class PresampConfigUtils {
    public static final Pattern ALIAS_TYPE_PATTERN = Pattern.compile("%([^%]+)%");

    public static Optional<AliasType> getAliasType(String aliasName) {
        switch (aliasName) {
            case "VCV":
            case "vcv":
                return Optional.of(AliasType.VCV);
            case "CVVC":
            case "cvvc":
                return Optional.of(AliasType.CVVC);
            case "BEGINNING_CV":
            case "BEGINING_CV": // It's mispelled in the original spec...
            case "beginning_cv":
            case "begining_cv":
                return Optional.of(AliasType.BEGINNING_CV);
            case "CROSS_CV":
            case "cross_cv":
                return Optional.of(AliasType.CROSS_CV);
            case "VC":
            case "vc":
                return Optional.of(AliasType.VC);
            case "CV":
            case "cv":
                return Optional.of(AliasType.CV);
            case "C":
            case "c":
                return Optional.of(AliasType.C);
            case "V":
            case "v":
                return Optional.of(AliasType.V);
            case "LONG_V":
            case "long_v":
                return Optional.of(AliasType.LONG_V);
            case "VCPAD":
            case "vcpad":
                return Optional.of(AliasType.VCPAD);
            case "VCVPAD":
            case "vcvpad":
                return Optional.of(AliasType.VCVPAD);
            case "ENDING1":
            case "ending1":
                return Optional.of(AliasType.ENDING_1);
            case "ENDING2":
            case "ending2":
                return Optional.of(AliasType.ENDING_2);
            default:
                return Optional.empty();
        }
    }
}
