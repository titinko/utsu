package com.utsusynth.utsu;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.utsusynth.utsu.controller.UtsuController;
import com.utsusynth.utsu.model.ModelModule;
import com.utsusynth.utsu.view.ViewModule;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.File;
import java.io.InputStream;

/**
 * UTAU-ish Thingy with Some Updates (UTSU)
 */
public class UtsuApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Set up Guice.
        Injector injector =
                Guice.createInjector(new UtsuModule(), new ModelModule(), new ViewModule());

        // Return an error if assets folder can't be found.
        File assetPath = injector.getInstance(Key.get(File.class, UtsuModule.AssetPath.class));
        if (!assetPath.exists()) {
            System.out.println("Error: assets directory not found.");
            System.out.println("Please cd to the parent directory of the assets folder.");
            primaryStage.show();
            primaryStage.close();
            return;
        }

        // Construct scene.
        FXMLLoader loader = injector.getInstance(FXMLLoader.class);
        InputStream fxml = getClass().getResourceAsStream("/fxml/UtsuScene.fxml");
        BorderPane pane = loader.load(fxml);
        Scene scene = new Scene(pane);
        scene.getStylesheets().add("/css/piano_roll.css");

        // Set the stage.
        primaryStage.setScene(scene);
        primaryStage.setTitle("Utsu");
        primaryStage.show();

        UtsuController controller = loader.getController();

        // Set up an event that runs every time a non-text-input key is pressed.
        primaryStage.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
            if (!(keyEvent.getTarget() instanceof TextInputControl)
                    || new KeyCodeCombination(KeyCode.TAB).match(keyEvent)) {
                if (controller.onKeyPressed(keyEvent)) {
                    keyEvent.consume();
                }
            }
        });

        // Set up an event that runs when the program is closed.
        primaryStage.setOnCloseRequest(windowEvent -> {
            if (!controller.onCloseWindow()) {
                windowEvent.consume();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
