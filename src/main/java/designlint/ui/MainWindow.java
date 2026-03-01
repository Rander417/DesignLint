package designlint.ui;

import designlint.core.AnalysisEngine;
import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
import sootup.core.model.SootClass;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The main window for DesignLint.
 *
 * === JAVAFX CONCEPTS (coming from Python/Tkinter or Swing) ===
 *
 * JavaFX uses a "scene graph" — a tree of UI nodes. Key concepts:
 *
 * - Stage: the window itself (like a JFrame in Swing, or Tk() in Tkinter)
 * - Scene: the content inside a window
 * - Node: any UI element — buttons, labels, text fields, containers, etc.
 * - Layout Panes: containers that arrange child nodes (VBox = vertical, HBox = horizontal,
 *   BorderPane = top/bottom/left/right/center, SplitPane = resizable split)
 * - Controls: interactive widgets (Button, ListView, CheckBox, TextArea, etc.)
 * - ObservableList: a list that automatically notifies the UI when items change
 *   (similar to Python's tkinter StringVar, but for collections)
 * - Properties: values that support binding and change listeners
 *
 * CSS styling is supported natively — you can style JavaFX apps with .css files,
 * which is much cleaner than Swing's programmatic look-and-feel system.
 */
public class MainWindow {

    private final Stage stage;
    private final AnalysisEngine engine;

    // --- UI Components ---
    private ListView<ClassItem> classListView;
    private ListView<GuidelineItem> guidelineListView;
    private TextFlow resultsDisplay;
    private ScrollPane resultsScrollPane;
    private Label statusBar;
    private Button loadButton;
    private Button analyzeButton;

    /**
     * Wrapper class for classes shown in the ListView.
     * Tracks the class name, whether it's user-selected, and whether the user
     * has checked it for analysis.
     */
    private static class ClassItem {
        final String className;
        final boolean isUserSelected;
        final SimpleBooleanProperty selected = new SimpleBooleanProperty(false);

        ClassItem(String className, boolean isUserSelected) {
            this.className = className;
            this.isUserSelected = isUserSelected;
        }

        @Override
        public String toString() {
            // The suffix helps users see which classes they loaded vs dependencies
            return className + (isUserSelected ? "" : "  [dependency]");
        }
    }

    /**
     * Wrapper for guidelines in the checkbox list.
     */
    private static class GuidelineItem {
        final DesignGuideline guideline;
        final SimpleBooleanProperty selected = new SimpleBooleanProperty(true); // on by default

        GuidelineItem(DesignGuideline guideline) {
            this.guideline = guideline;
        }

        @Override
        public String toString() {
            return guideline.name();
        }
    }

    public MainWindow(Stage stage, AnalysisEngine engine) {
        this.stage = stage;
        this.engine = engine;
        buildUI();
    }

    private void buildUI() {
        // === TOP: Toolbar with buttons ===
        loadButton = new Button("Load Classes...");
        loadButton.setOnAction(e -> handleLoad());

        analyzeButton = new Button("Run Analysis");
        analyzeButton.setDisable(true); // disabled until classes are loaded
        analyzeButton.setOnAction(e -> handleAnalyze());

        Button selectAllClasses = new Button("Select All");
        selectAllClasses.setOnAction(e -> classListView.getItems().forEach(i -> i.selected.set(true)));

        Button deselectAllClasses = new Button("Deselect All");
        deselectAllClasses.setOnAction(e -> classListView.getItems().forEach(i -> i.selected.set(false)));

        ToolBar toolBar = new ToolBar(
                loadButton,
                new Separator(),
                selectAllClasses,
                deselectAllClasses,
                new Separator(),
                analyzeButton
        );

        // === LEFT PANEL: Class list + Guideline checkboxes ===

        // Class list with checkboxes
        Label classLabel = new Label("Loaded Classes:");
        classLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        classListView = new ListView<>();
        // CheckBoxListCell provides a checkbox for each item, bound to the selected property
        classListView.setCellFactory(CheckBoxListCell.forListView(item -> item.selected));
        classListView.setPrefHeight(300);

        // Guideline checkboxes
        Label guidelineLabel = new Label("Design Guidelines:");
        guidelineLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        guidelineListView = new ListView<>();
        guidelineListView.setCellFactory(CheckBoxListCell.forListView(item -> item.selected));
        guidelineListView.setPrefHeight(150);

        // Populate guidelines from the engine
        ObservableList<GuidelineItem> guidelineItems = FXCollections.observableArrayList(
                engine.getAvailableGuidelines().stream()
                        .map(GuidelineItem::new)
                        .collect(Collectors.toList())
        );
        guidelineListView.setItems(guidelineItems);

        // Tooltip for each guideline (shown on hover)
        guidelineListView.setCellFactory(lv -> {
            CheckBoxListCell<GuidelineItem> cell = new CheckBoxListCell<>(item -> item.selected);
            cell.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null) {
                    cell.setTooltip(new Tooltip(newItem.guideline.description()));
                }
            });
            return cell;
        });

        VBox leftPanel = new VBox(8,
                classLabel, classListView,
                new Separator(),
                guidelineLabel, guidelineListView
        );
        leftPanel.setPadding(new Insets(8));
        leftPanel.setPrefWidth(350);

        // === RIGHT PANEL: Results display ===
        Label resultsLabel = new Label("Analysis Results:");
        resultsLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        resultsDisplay = new TextFlow();
        resultsDisplay.setPadding(new Insets(8));
        resultsDisplay.setLineSpacing(4);

        resultsScrollPane = new ScrollPane(resultsDisplay);
        resultsScrollPane.setFitToWidth(true);
        // VBox.setVgrow tells the layout to expand this component to fill available space
        VBox.setVgrow(resultsScrollPane, Priority.ALWAYS);

        VBox rightPanel = new VBox(8, resultsLabel, resultsScrollPane);
        rightPanel.setPadding(new Insets(8));

        // === SPLIT PANE: Left | Right ===
        SplitPane splitPane = new SplitPane(leftPanel, rightPanel);
        splitPane.setDividerPositions(0.35); // 35% left, 65% right

        // === BOTTOM: Status bar ===
        statusBar = new Label("Ready. Load classes to begin analysis.");
        statusBar.setPadding(new Insets(4, 8, 4, 8));
        statusBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc; " +
                           "-fx-border-width: 1 0 0 0;");

        // === ROOT LAYOUT ===
        // BorderPane is the classic "header / sidebar / content / footer" layout
        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(splitPane);
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 950, 650);

        // Apply custom CSS styling
        scene.getStylesheets().add(getClass().getResource("/styles.css") != null
                ? getClass().getResource("/styles.css").toExternalForm()
                : "");

        stage.setScene(scene);
        stage.setTitle("DesignLint — Java Design Guideline Analyzer");
        stage.show();
    }

    /**
     * Handle the "Load Classes" button click.
     * Opens a directory chooser (since .class files are typically in a directory tree).
     */
    private void handleLoad() {
        // Give the user a choice: load a directory or a JAR file
        // DirectoryChooser for directories, FileChooser for JARs
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Directory Containing .class Files");

        File selectedDir = dirChooser.showDialog(stage);
        if (selectedDir == null) {
            return; // user cancelled
        }

        statusBar.setText("Loading classes from: " + selectedDir.getAbsolutePath() + "...");

        try {
            // Load classes using SootUp via our service
            engine.getClassLoadingService().load(selectedDir.toPath(), Set.of());

            // Populate the class list
            ObservableList<ClassItem> classItems = FXCollections.observableArrayList();

            // User-selected classes first (visually distinct)
            for (SootClass sc : engine.getClassLoadingService().getUserSelectedClasses()) {
                var item = new ClassItem(sc.getType().getFullyQualifiedName(), true);
                item.selected.set(true); // pre-select user's classes
                classItems.add(item);
            }
            // Then dependency classes
            for (SootClass sc : engine.getClassLoadingService().getDependencyClasses()) {
                classItems.add(new ClassItem(sc.getType().getFullyQualifiedName(), false));
            }

            classListView.setItems(classItems);
            analyzeButton.setDisable(false);

            int count = engine.getClassLoadingService().classCount();
            statusBar.setText("Loaded " + count + " class(es). Select classes and guidelines, then run analysis.");

        } catch (Exception ex) {
            showError("Failed to load classes", ex.getMessage());
            statusBar.setText("Error loading classes. See error dialog.");
        }
    }

    /**
     * Handle the "Run Analysis" button click.
     * Runs selected guidelines against selected classes and displays results.
     */
    private void handleAnalyze() {
        // Gather selected class names
        Set<String> selectedClasses = classListView.getItems().stream()
                .filter(item -> item.selected.get())
                .map(item -> item.className)
                .collect(Collectors.toSet());

        // Gather selected guideline names
        Set<String> selectedGuidelines = guidelineListView.getItems().stream()
                .filter(item -> item.selected.get())
                .map(item -> item.guideline.name())
                .collect(Collectors.toSet());

        if (selectedClasses.isEmpty()) {
            showError("No Classes Selected", "Please select at least one class to analyze.");
            return;
        }
        if (selectedGuidelines.isEmpty()) {
            showError("No Guidelines Selected", "Please select at least one design guideline to check.");
            return;
        }

        statusBar.setText("Analyzing " + selectedClasses.size() + " class(es) with " +
                          selectedGuidelines.size() + " guideline(s)...");

        try {
            List<AnalysisResult> results = engine.analyze(selectedClasses, selectedGuidelines);
            displayResults(results);

            long violations = results.stream().filter(r -> !r.passed()).count();
            long passes = results.stream().filter(AnalysisResult::passed).count();
            statusBar.setText("Analysis complete: " + violations + " violation(s), " +
                              passes + " pass(es).");

        } catch (Exception ex) {
            showError("Analysis Error", ex.getMessage());
            statusBar.setText("Error during analysis. See error dialog.");
        }
    }

    /**
     * Display analysis results in the TextFlow with color-coded formatting.
     * Violations in red, passes in green.
     */
    private void displayResults(List<AnalysisResult> results) {
        resultsDisplay.getChildren().clear();

        if (results.isEmpty()) {
            addResultText("No results to display.\n", Color.GRAY, false);
            return;
        }

        // Group results by class name for readability
        Map<String, List<AnalysisResult>> byClass = results.stream()
                .collect(Collectors.groupingBy(AnalysisResult::className, LinkedHashMap::new, Collectors.toList()));

        for (var entry : byClass.entrySet()) {
            // Class name header
            addResultText("\n" + entry.getKey() + "\n", Color.DARKBLUE, true);

            for (AnalysisResult result : entry.getValue()) {
                if (result instanceof AnalysisResult.Violation v) {
                    // Pattern matching with variable binding (Java 16+)
                    addResultText("  ✗ ", Color.RED, true);
                    addResultText("[" + v.guidelineName() + "] ", Color.DARKRED, true);
                    addResultText(v.message() + "\n", Color.DARKRED, false);

                } else if (result instanceof AnalysisResult.Pass p) {
                    addResultText("  ✓ ", Color.GREEN, true);
                    addResultText("[" + p.guidelineName() + "] ", Color.DARKGREEN, true);
                    addResultText("OK\n", Color.DARKGREEN, false);
                }
            }
        }

        // Scroll to top
        resultsScrollPane.setVvalue(0);
    }

    /** Helper: add styled text to the results display. */
    private void addResultText(String content, Color color, boolean bold) {
        Text text = new Text(content);
        text.setFill(color);
        if (bold) {
            text.setFont(Font.font("System", FontWeight.BOLD, 12));
        } else {
            text.setFont(Font.font("System", 12));
        }
        resultsDisplay.getChildren().add(text);
    }

    /** Show an error dialog. */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
