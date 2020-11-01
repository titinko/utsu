package com.utsusynth.utsu.model.config

import javafx.scene.paint.Color
import java.util.Collections.emptyMap

data class Theme(val id: String) {
    var name: String = id;
    var colorMap: Map<String, Color> = emptyMap();
}