package com.utsusynth.utsu.view.song.note.portamento;

public class CurveFactory {
    public Curve createCurve(
            double startX,
            double startY,
            double endX,
            double endY,
            String curveType) {
        switch (curveType) {
            case "r":
                return new RCurve(startX, startY, endX, endY);
            case "j":
                return new JCurve(startX, startY, endX, endY);
            case "s":
                return new StraightCurve(startX, startY, endX, endY);
            default:
                return new SCurve(startX, startY, endX, endY);
        }
    }
}
