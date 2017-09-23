package com.utsusynth.utsu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.util.ResourceBundle;

import org.apache.commons.io.FileUtils;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.utsusynth.utsu.common.QuantizedAddRequest;
import com.utsusynth.utsu.common.QuantizedAddResponse;
import com.utsusynth.utsu.common.QuantizedNote;
import com.utsusynth.utsu.common.Quantizer;
import com.utsusynth.utsu.common.exception.NoteAlreadyExistsException;
import com.utsusynth.utsu.common.i18n.Localizable;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.i18n.NativeLocale;
import com.utsusynth.utsu.engine.Engine;
import com.utsusynth.utsu.files.Ust12Reader;
import com.utsusynth.utsu.files.Ust12Writer;
import com.utsusynth.utsu.files.Ust20Reader;
import com.utsusynth.utsu.files.Ust20Writer;
import com.utsusynth.utsu.model.SongManager;
import com.utsusynth.utsu.view.Piano;
import com.utsusynth.utsu.view.Track;
import com.utsusynth.utsu.view.ViewCallback;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * 'UtsuScene.fxml' Controller Class
 */
public class UtsuController implements Localizable {
	public enum Mode {
		ADD,
		EDIT,
		DELETE,
	}

	// User session data goes here.
	private Mode currentMode;
	
	// Helper classes go here.
	private final SongManager songManager;
	private final Engine engine;
	private final Track track;
	private final Piano piano;
	private final Localizer localizer;
	private final Quantizer quantizer;
	private final Ust12Reader ust12Reader;
	private final Ust12Writer ust12Writer;
	private final Ust20Reader ust20Reader;
	private final Ust20Writer ust20Writer;
	private final Provider<FXMLLoader> fxmlLoaders;
	
	@FXML // fx:id="scrollPaneLeft"
	private ScrollPane scrollPaneLeft; // Value injected by FXMLLoader
	
	@FXML // fx:id="anchorLeft"
	private AnchorPane anchorLeft; // Value injected by FXMLLoader
	
	@FXML // fx:id="scrollPaneRight"
	private ScrollPane scrollPaneRight; // Value injected by FXMLLoader
	
	@FXML // fx:id="anchorRight"
	private AnchorPane anchorRight; // Value injected by FXMLLoader
    
	@FXML // fx:id="voicebankImage"
	private ImageView voicebankImage; // Value injected by FXMLLoader
	
    @FXML // fx:id="modeChoiceBox"
    private ChoiceBox<Mode> modeChoiceBox; // Value injected by FXMLLoader
    
    @FXML // fx:id="quantizeChoiceBox"
    private ChoiceBox<String> quantizeChoiceBox; // Value injected by FXMLLoader
    
    @FXML // fx:id="languageChoiceBox"
    private ChoiceBox<NativeLocale> languageChoiceBox; // Value injected by FXMLLoader
    
    @Inject
    public UtsuController(
    		SongManager songManager,
    		Engine engine,
    		Track track,
    		Piano piano,
    		Localizer localizer,
    		Quantizer quantizer,
    		Ust12Reader ust12Reader,
    		Ust12Writer ust12Writer,
    		Ust20Reader ust20Reader,
    		Ust20Writer ust20Writer,
    		Provider<FXMLLoader> fxmlLoaders) {
    	this.songManager = songManager;
    	this.engine = engine;
    	this.track = track;
    	this.piano = piano;
    	this.localizer = localizer;
    	this.quantizer = quantizer;
    	this.ust12Reader = ust12Reader;
    	this.ust12Writer = ust12Writer;
    	this.ust20Reader = ust20Reader;
    	this.ust20Writer = ust20Writer;
    	this.fxmlLoaders = fxmlLoaders;
    }
    
    // Provide setup for other controllers.
    public void initialize() {
    	track.initialize(new ViewCallback() {
			@Override
			public QuantizedAddResponse addNote(QuantizedAddRequest request)
					throws NoteAlreadyExistsException {
				return songManager.getSong().addNote(request);
			}
			@Override
			public QuantizedAddResponse removeNote(QuantizedNote toRemove) {
				return songManager.getSong().removeNote(toRemove);
			}
			@Override
			public Mode getCurrentMode() {
				return currentMode;
			}
    	});
    	anchorLeft.getChildren().add(piano.getElement());
    	scrollPaneLeft.vvalueProperty().bindBidirectional(scrollPaneRight.vvalueProperty());
    	
    	modeChoiceBox.setItems(FXCollections.observableArrayList(Mode.ADD, Mode.EDIT, Mode.DELETE));
    	modeChoiceBox.setOnAction((action) -> {
    		currentMode = modeChoiceBox.getValue();
    	});
    	modeChoiceBox.setValue(Mode.ADD);
    	quantizeChoiceBox.setItems(
    			FXCollections.observableArrayList("1 per beat", "2 per beat", "4 per beat"));
    	quantizeChoiceBox.setOnAction((action) -> {
    		String quantization = quantizeChoiceBox.getValue();
    		if (quantization.equals("1 per beat")) {
    			quantizer.changeQuant(quantizer.getQuant(), 1);
    		} else if (quantization.equals("2 per beat")) {
    			quantizer.changeQuant(quantizer.getQuant(), 2);
    		} else if (quantization.equals("4 per beat")) {
    			quantizer.changeQuant(quantizer.getQuant(), 4);
    		}
    	});
    	quantizeChoiceBox.setValue("1 per beat");
    	
    	languageChoiceBox.setItems(FXCollections.observableArrayList(localizer.getAllLocales()));
    	languageChoiceBox.setOnAction(
    			(action) -> localizer.setLocale(languageChoiceBox.getValue()));
    	languageChoiceBox.setValue(localizer.getCurrentLocale());
    	
    	refreshView();
    	
    	// Set up localization.
    	localizer.localize(this);
    }
    
    @FXML private Menu fileMenu; // Value injected by FXMLLoader
    @FXML private MenuItem openItem; // Value injected by FXMLLoader
    @FXML private MenuItem saveAsItem; // Value injected by FXMLLoader
    @FXML private Menu editMenu; // Value injected by FXMLLoader
    @FXML private Menu projectMenu; // Value injected by FXMLLoader
    @FXML private MenuItem propertiesItem; // Value injected by FXMLLoader
    @FXML private Menu helpMenu; // Value injected by FXMLLoader
    @FXML private MenuItem aboutItem; // Value injected by FXMLLoader
    @FXML private Label modeLabel; // Value injected by FXMLLoader
    @FXML private Label quantizationLabel; // Value injected by FXMLLoader
    @FXML private Button renderButton; // Value injected by FXMLLoader
    @FXML private Button exportWavButton; // Value injected by FXMLLoader
    
    @Override
    public void localize(ResourceBundle bundle) {
    	fileMenu.setText(bundle.getString("menu.file"));
    	openItem.setText(bundle.getString("menu.file.open"));
    	saveAsItem.setText(bundle.getString("menu.file.saveAs"));
    	editMenu.setText(bundle.getString("menu.edit"));
    	projectMenu.setText(bundle.getString("menu.project"));
    	propertiesItem.setText(bundle.getString("menu.project.properties"));
    	helpMenu.setText(bundle.getString("menu.help"));
    	aboutItem.setText(bundle.getString("menu.help.about"));
    	modeLabel.setText(bundle.getString("top.mode"));
    	quantizationLabel.setText(bundle.getString("top.quantization"));
    	renderButton.setText(bundle.getString("top.render"));
    	exportWavButton.setText(bundle.getString("top.exportWav"));
    	
    	// Force the menu to refresh.
    	fileMenu.setVisible(false);
    	fileMenu.setVisible(true);
    }
    
    private void refreshView() {
    	// Set song image.
    	Image image = new Image("file:" + songManager.getSong().getVoicebank().getImagePath());
    	voicebankImage.setImage(image);
    	
    	anchorRight.getChildren().clear();
		anchorRight.getChildren().add(
				track.createNewTrack(songManager.getSong().getQuantizedNotes()));
    }

    @FXML
    void openFile(ActionEvent event) {
    	FileChooser fc = new FileChooser();
    	fc.setTitle("Select UST File");
    	fc.getExtensionFilters().addAll(
    			new ExtensionFilter("UST files", "*.ust"),
    			new ExtensionFilter("All files", "*.*"));
    	File file = fc.showOpenDialog(null);
    	if (file != null) {
    		try {
    			String charset = "UTF-8";
    			CharsetDecoder utf8Decoder = Charset.forName("UTF-8").newDecoder()
    	    			.onMalformedInput(CodingErrorAction.REPORT)
    	    			.onUnmappableCharacter(CodingErrorAction.REPORT);
    			try {
    				utf8Decoder.decode(ByteBuffer.wrap(FileUtils.readFileToByteArray(file)));
    			} catch (MalformedInputException | UnmappableCharacterException e) {
    				charset = "SJIS";
    			}
    			String content = FileUtils.readFileToString(file, charset);
    			if (content.contains("UST Version1.2")) {
    				songManager.setSong(ust12Reader.loadSong(content));
    			} else if (content.contains("UST Version2.0")) {
    				songManager.setSong(ust20Reader.loadSong(content));
    			} else {
    				// TODO: Deal with this error.
    				System.out.println("UST format not found!");
    				return;
    			}
    			//System.out.println(currentSong);
    			refreshView();
    		} catch (IOException e) {
				// TODO Handle this.
				e.printStackTrace();
			}
    	}
    }

    @FXML
    void saveFile(ActionEvent event) {
    	FileChooser fc = new FileChooser();
    	fc.setTitle("Select UST File");
    	fc.getExtensionFilters().addAll(
    			new ExtensionFilter("UST 2.0 (UTF-8)", "*.ust"),
    			new ExtensionFilter("UST 2.0 (Shift JIS)", "*.ust"),
    			new ExtensionFilter("UST 1.2 (Shift JIS)", "*.ust"));
    	File file = fc.showSaveDialog(null);
    	if (file != null) {
    		ExtensionFilter chosenFormat = fc.getSelectedExtensionFilter();
    		String charset = "UTF-8";
    		if (chosenFormat.getDescription().contains("Shift JIS")) {
    			charset = "SJIS";
    		}
    		try (PrintStream ps = new PrintStream(file, charset)) {
    			if (chosenFormat.getDescription().contains("UST 1.2")) {
    				ust12Writer.writeSong(songManager.getSong(), ps);
    			} else {
    				ust20Writer.writeSong(songManager.getSong(), ps, charset);
    			}
    			ps.close();
    		} catch (FileNotFoundException | UnsupportedEncodingException e) {
    			// TODO: Handle this.
    			e.printStackTrace();
    		}
    	}
    }
    
    @FXML
    void renderSong(ActionEvent event) {
    	engine.render(songManager.getSong(), Optional.absent());
    }
    
    @FXML
    void exportSongAsWav(ActionEvent event) {
    	FileChooser fc = new FileChooser();
    	fc.setTitle("Select WAV File");
    	fc.getExtensionFilters().addAll(new ExtensionFilter(".wav files", "*.wav"));
    	File file = fc.showSaveDialog(null);
    	if (file != null) {
    		engine.render(songManager.getSong(), Optional.of(file));
    	}
    }

    @FXML
    void openProperties(ActionEvent event) {  
    	// Open properties modal.
    	InputStream fxml = getClass().getResourceAsStream("/fxml/PropertiesScene.fxml");
    	FXMLLoader loader = fxmlLoaders.get();
    	try {
    		Stage currentStage = (Stage) anchorRight.getScene().getWindow();
    		Stage propertiesWindow = new Stage();
    		propertiesWindow.initModality(Modality.APPLICATION_MODAL);
    		propertiesWindow.initOwner(currentStage);
			BorderPane propertiesPane = loader.load(fxml);
			propertiesWindow.setScene(new Scene(propertiesPane));
			propertiesWindow.showAndWait();
		} catch (IOException e) {
			// TODO Handle this.
			e.printStackTrace();
		}
    	refreshView();
    }
}
