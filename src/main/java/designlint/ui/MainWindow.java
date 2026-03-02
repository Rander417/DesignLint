package designlint.ui;

import designlint.core.AnalysisEngine;
import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
import designlint.core.Severity;
import sootup.core.model.SootClass;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
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
 *
 * === RESULTS DISPLAY (Phase 1 & 2 enhancements) ===
 *
 * Results are now severity-aware:
 *   - Color-coded by severity: ERROR (red), WARNING (amber), ADVISORY (blue)
 *   - Summary banner shows counts at a glance
 *   - Filter toggles let users show/hide by severity level and passes
 *   - Sorting by severity-first (errors float to top) or class-first (original behavior)
 *
 * The key architectural change: results are stored after analysis so that filter/sort
 * changes can re-render without re-running analysis. The refreshResults() method reads
 * from this stored list and applies the current filter/sort state.
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

    // --- Results display: summary, filter, and sort controls (Phase 1 & 2) ---
    private HBox summaryBar;
    private ToggleButton filterError;
    private ToggleButton filterWarning;
    private ToggleButton filterAdvisory;
    private ToggleButton filterPasses;
    private ToggleButton sortBySeverity;

    /** Stored results from the last analysis run, used for re-filtering/sorting. */
    private List<AnalysisResult> lastResults = List.of();

    // --- Severity color palette ---
    // These are used in Java code for TextFlow rendering. Matching CSS classes
    // exist in styles.css for the toggle buttons and summary badges.
    private static final Color COLOR_ERROR      = Color.web("#c62828");   // deep red
    private static final Color COLOR_ERROR_ICON = Color.web("#d32f2f");   // brighter red for icon
    private static final Color COLOR_WARN       = Color.web("#e65100");   // deep amber
    private static final Color COLOR_WARN_ICON  = Color.web("#f57c00");   // brighter amber for icon
    private static final Color COLOR_ADVISORY      = Color.web("#1565c0");   // deep blue
    private static final Color COLOR_ADVISORY_ICON = Color.web("#1976d2");   // brighter blue for icon
    private static final Color COLOR_PASS       = Color.web("#2e7d32");   // deep green
    private static final Color COLOR_PASS_ICON  = Color.web("#388e3c");   // brighter green for icon
    private static final Color COLOR_CLASS_HDR  = Color.web("#1a237e");   // dark indigo for class names

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
            return className + (isUserSelected ? "" : "  [dependency]");
        }
    }

    /**
     * Wrapper for guidelines in the checkbox list.
     */
    private static class GuidelineItem {
        final DesignGuideline guideline;
        final SimpleBooleanProperty selected = new SimpleBooleanProperty(true);

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
        analyzeButton.setDisable(true);
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

        Label classLabel = new Label("Loaded Classes:");
        classLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        classListView = new ListView<>();
        classListView.setCellFactory(CheckBoxListCell.forListView(item -> item.selected));
        classListView.setPrefHeight(300);

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

        // === RIGHT PANEL: Results display with filter controls ===
        Label resultsLabel = new Label("Analysis Results:");
        resultsLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        // --- Summary banner (Phase 1) ---
        // Shows at-a-glance counts like: "● 3 Errors  ● 7 Warnings  ● 2 Advisories  ✓ 12 Passes"
        summaryBar = new HBox(6);
        summaryBar.setAlignment(Pos.CENTER_LEFT);
        summaryBar.setPadding(new Insets(6, 10, 6, 10));
        summaryBar.getStyleClass().add("summary-bar");
        summaryBar.setVisible(false);    // hidden until first analysis
        summaryBar.setManaged(false);    // don't take up layout space when hidden

        // --- Filter & sort controls (Phase 2) ---
        // ToggleButtons act as on/off switches: pressed = showing that category.
        // All start "selected" (pressed) so everything is visible by default.
        filterError = createFilterToggle("Errors", "filter-error");
        filterWarning = createFilterToggle("Warnings", "filter-warning");
        filterAdvisory = createFilterToggle("Advisories", "filter-advisory");
        filterPasses = createFilterToggle("Passes", "filter-pass");

        // Sort toggle: unpressed = group by class (default), pressed = severity first
        sortBySeverity = new ToggleButton("Sort by Severity");
        sortBySeverity.getStyleClass().add("filter-toggle");
        sortBySeverity.getStyleClass().add("sort-toggle");
        sortBySeverity.setSelected(false);
        sortBySeverity.setTooltip(new Tooltip(
                "Off: group results by class name\nOn: errors first, then warnings, advisories, passes"));
        sortBySeverity.setOnAction(e -> refreshResults());

        Label showLabel = new Label("Show:");
        showLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");

        HBox filterBar = new HBox(6,
                showLabel,
                filterError, filterWarning, filterAdvisory, filterPasses,
                new Separator(Orientation.VERTICAL),
                sortBySeverity
        );
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(4, 8, 4, 8));
        filterBar.getStyleClass().add("filter-bar");

        // --- Results TextFlow ---
        resultsDisplay = new TextFlow();
        resultsDisplay.setPadding(new Insets(8));
        resultsDisplay.setLineSpacing(4);

        resultsScrollPane = new ScrollPane(resultsDisplay);
        resultsScrollPane.setFitToWidth(true);
        VBox.setVgrow(resultsScrollPane, Priority.ALWAYS);

        VBox rightPanel = new VBox(8,
                resultsLabel,
                summaryBar,
                filterBar,
                resultsScrollPane
        );
        rightPanel.setPadding(new Insets(8));

        // === SPLIT PANE: Left | Right ===
        SplitPane splitPane = new SplitPane(leftPanel, rightPanel);
        splitPane.setDividerPositions(0.35);

        // === BOTTOM: Status bar ===
        statusBar = new Label("Ready. Load classes to begin analysis.");
        statusBar.setPadding(new Insets(4, 8, 4, 8));
        statusBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc; " +
                           "-fx-border-width: 1 0 0 0;");

        // === ROOT LAYOUT ===
        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(splitPane);
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1000, 700);

        scene.getStylesheets().add(getClass().getResource("/styles.css") != null
                ? getClass().getResource("/styles.css").toExternalForm()
                : "");

        stage.setScene(scene);
        stage.setTitle("DesignLint \u2014 Java Design Guideline Analyzer");
        stage.show();
    }

    /**
     * Create a filter toggle button with consistent styling.
     *
     * ToggleButton is like a checkbox shaped as a button — it stays pressed/unpressed.
     * Each filter starts selected (showing that category). Clicking toggles visibility
     * of that result type and re-renders without re-running analysis.
     */
    private ToggleButton createFilterToggle(String label, String styleClass) {
        ToggleButton toggle = new ToggleButton(label);
        toggle.setSelected(true);
        toggle.getStyleClass().addAll("filter-toggle", styleClass);
        toggle.setOnAction(e -> refreshResults());
        return toggle;
    }

    /**
     * Handle the "Load Classes" button click.
     */
    private void handleLoad() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Directory Containing .class Files");

        File selectedDir = dirChooser.showDialog(stage);
        if (selectedDir == null) {
            return;
        }

        statusBar.setText("Loading classes from: " + selectedDir.getAbsolutePath() + "...");

        try {
            engine.getClassLoadingService().load(selectedDir.toPath(), Set.of());

            ObservableList<ClassItem> classItems = FXCollections.observableArrayList();

            for (SootClass sc : engine.getClassLoadingService().getUserSelectedClasses()) {
                var item = new ClassItem(sc.getType().getFullyQualifiedName(), true);
                item.selected.set(true);
                classItems.add(item);
            }
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
     * Stores results for re-filtering, then renders.
     */
    private void handleAnalyze() {
        Set<String> selectedClasses = classListView.getItems().stream()
                .filter(item -> item.selected.get())
                .map(item -> item.className)
                .collect(Collectors.toSet());

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
            lastResults = engine.analyze(selectedClasses, selectedGuidelines);
            refreshResults();

        } catch (Exception ex) {
            showError("Analysis Error", ex.getMessage());
            statusBar.setText("Error during analysis. See error dialog.");
        }
    }

    // ==================================================================================
    // Results rendering — Phase 1 (severity display) & Phase 2 (filtering/sorting)
    // ==================================================================================

    /**
     * Re-render results using stored data and current filter/sort state.
     *
     * This is the key architectural change: analysis runs once, but the display
     * can be re-rendered any time a filter toggle or sort toggle changes.
     */
    private void refreshResults() {
        if (lastResults.isEmpty()) {
            displayEmptyResults();
            return;
        }

        // Always update summary with unfiltered counts
        updateSummary(lastResults);

        // Apply filters
        List<AnalysisResult> filtered = lastResults.stream()
                .filter(this::passesFilter)
                .collect(Collectors.toList());

        // Apply sort
        List<AnalysisResult> sorted;
        if (sortBySeverity.isSelected()) {
            sorted = sortBySeverityThenClass(filtered);
        } else {
            sorted = sortByClassThenSeverity(filtered);
        }

        // Render
        displayResults(sorted);

        // Update status bar with filter awareness
        long totalViolations = lastResults.stream().filter(r -> !r.passed()).count();
        long totalPasses = lastResults.stream().filter(AnalysisResult::passed).count();
        long showing = filtered.size();
        long total = lastResults.size();

        String statusText = "Analysis complete: " + totalViolations + " violation(s), " +
                            totalPasses + " pass(es).";
        if (showing < total) {
            statusText += "  (Showing " + showing + " of " + total + " results)";
        }
        statusBar.setText(statusText);
    }

    /** Check whether a result passes the current filter settings. */
    private boolean passesFilter(AnalysisResult result) {
        if (result instanceof AnalysisResult.Pass) {
            return filterPasses.isSelected();
        } else if (result instanceof AnalysisResult.Violation v) {
            return switch (v.severity()) {
                case ERROR    -> filterError.isSelected();
                case WARNING  -> filterWarning.isSelected();
                case ADVISORY -> filterAdvisory.isSelected();
            };
        }
        return true;
    }

    /** Sort by severity (errors first), then by class name within each severity group. */
    private List<AnalysisResult> sortBySeverityThenClass(List<AnalysisResult> results) {
        return results.stream()
                .sorted(Comparator
                        .comparingInt(this::severityOrdinal)
                        .thenComparing(AnalysisResult::className)
                        .thenComparing(AnalysisResult::guidelineName))
                .collect(Collectors.toList());
    }

    /** Sort by class name first (default), then severity within each class. */
    private List<AnalysisResult> sortByClassThenSeverity(List<AnalysisResult> results) {
        return results.stream()
                .sorted(Comparator
                        .comparing(AnalysisResult::className)
                        .thenComparingInt(this::severityOrdinal)
                        .thenComparing(AnalysisResult::guidelineName))
                .collect(Collectors.toList());
    }

    /**
     * Numeric sort key for severity. Lower = more critical = sorts first.
     * ERROR=0, WARNING=1, ADVISORY=2, PASS=3.
     */
    private int severityOrdinal(AnalysisResult result) {
        if (result instanceof AnalysisResult.Violation v) {
            return v.severity().ordinal();
        }
        return 3; // Passes sort last
    }

    /**
     * Update the summary banner with colored badges from unfiltered results.
     * Example: [● 3 Errors] [● 7 Warnings] [● 2 Advisories] [✓ 12 Passed]
     */
    private void updateSummary(List<AnalysisResult> allResults) {
        long errors = allResults.stream()
                .filter(r -> r instanceof AnalysisResult.Violation v && v.severity() == Severity.ERROR)
                .count();
        long warnings = allResults.stream()
                .filter(r -> r instanceof AnalysisResult.Violation v && v.severity() == Severity.WARNING)
                .count();
        long advisories = allResults.stream()
                .filter(r -> r instanceof AnalysisResult.Violation v && v.severity() == Severity.ADVISORY)
                .count();
        long passes = allResults.stream()
                .filter(AnalysisResult::passed)
                .count();

        summaryBar.getChildren().clear();

        // Only show non-zero badges to avoid clutter
        if (errors > 0) {
            summaryBar.getChildren().add(
                    createBadge("\u25cf " + errors + " Error" + plural(errors), "badge-error"));
        }
        if (warnings > 0) {
            summaryBar.getChildren().add(
                    createBadge("\u25cf " + warnings + " Warning" + plural(warnings), "badge-warning"));
        }
        if (advisories > 0) {
            String advText = advisories == 1 ? "Advisory" : "Advisories";
            summaryBar.getChildren().add(
                    createBadge("\u25cf " + advisories + " " + advText, "badge-advisory"));
        }
        if (passes > 0) {
            summaryBar.getChildren().add(
                    createBadge("\u2713 " + passes + " Passed", "badge-pass"));
        }

        summaryBar.setVisible(true);
        summaryBar.setManaged(true);
    }

    /** Create a styled badge label for the summary bar. */
    private Label createBadge(String text, String styleClass) {
        Label badge = new Label(text);
        badge.getStyleClass().addAll("summary-badge", styleClass);
        badge.setPadding(new Insets(2, 8, 2, 8));
        return badge;
    }

    /** Returns "s" for counts != 1, empty string otherwise. */
    private String plural(long count) {
        return count == 1 ? "" : "s";
    }

    /**
     * Render results into the TextFlow with severity-aware colors and icons.
     *
     * Layout differs based on sort mode:
     *   - Class-first (default): groups under class name headers
     *   - Severity-first: groups under severity level headers
     */
    private void displayResults(List<AnalysisResult> results) {
        resultsDisplay.getChildren().clear();

        if (results.isEmpty()) {
            addResultText("All results filtered out. Adjust filters above.\n", Color.GRAY, false);
            return;
        }

        if (sortBySeverity.isSelected()) {
            renderBySeverity(results);
        } else {
            renderByClass(results);
        }

        resultsScrollPane.setVvalue(0);
    }

    /** Render grouped by class name (default view). */
    private void renderByClass(List<AnalysisResult> results) {
        Map<String, List<AnalysisResult>> byClass = results.stream()
                .collect(Collectors.groupingBy(AnalysisResult::className,
                        LinkedHashMap::new, Collectors.toList()));

        for (var entry : byClass.entrySet()) {
            addResultText("\n" + entry.getKey() + "\n", COLOR_CLASS_HDR, true);
            for (AnalysisResult result : entry.getValue()) {
                renderSingleResult(result);
            }
        }
    }

    /** Render grouped by severity level (errors first). */
    private void renderBySeverity(List<AnalysisResult> results) {
        Map<String, List<AnalysisResult>> grouped = new LinkedHashMap<>();

        for (AnalysisResult result : results) {
            String group = getSeverityGroupLabel(result);
            grouped.computeIfAbsent(group, k -> new ArrayList<>()).add(result);
        }

        for (var entry : grouped.entrySet()) {
            addResultText("\n\u2501\u2501 " + entry.getKey() + " \u2501\u2501\n",
                    Color.web("#555555"), true);
            for (AnalysisResult result : entry.getValue()) {
                renderSingleResult(result);
            }
        }
    }

    /** Get a display label for severity-based grouping. */
    private String getSeverityGroupLabel(AnalysisResult result) {
        if (result instanceof AnalysisResult.Violation v) {
            return switch (v.severity()) {
                case ERROR    -> "Errors";
                case WARNING  -> "Warnings";
                case ADVISORY -> "Advisories";
            };
        }
        return "Passed";
    }

    /**
     * Render a single result line with severity-appropriate icon, color, and label.
     *
     * Examples:
     *   ✗ [ERROR] [equals/hashCode Check] Class overrides equals() but not hashCode()
     *   ⚠ [WARNING] [Serializable Check] Implements Serializable without serialVersionUID
     *   ℹ [ADVISORY] [toString Check] Does not override toString()
     *   ✓ [Cloneable Check] OK
     */
    private void renderSingleResult(AnalysisResult result) {
        if (result instanceof AnalysisResult.Violation v) {
            Color iconColor;
            Color textColor;
            String icon;
            String severityTag;

            switch (v.severity()) {
                case ERROR -> {
                    icon = "  \u2717 ";
                    iconColor = COLOR_ERROR_ICON;
                    textColor = COLOR_ERROR;
                    severityTag = "ERROR";
                }
                case WARNING -> {
                    icon = "  \u26a0 ";
                    iconColor = COLOR_WARN_ICON;
                    textColor = COLOR_WARN;
                    severityTag = "WARNING";
                }
                case ADVISORY -> {
                    icon = "  \u2139 ";
                    iconColor = COLOR_ADVISORY_ICON;
                    textColor = COLOR_ADVISORY;
                    severityTag = "ADVISORY";
                }
                default -> {
                    icon = "  \u2717 ";
                    iconColor = COLOR_ERROR_ICON;
                    textColor = COLOR_ERROR;
                    severityTag = "ERROR";
                }
            }

            addResultText(icon, iconColor, true);
            addResultText("[" + severityTag + "] ", textColor, true);
            addResultText("[" + v.guidelineName() + "] ", textColor, true);
            addResultText(v.message() + "\n", textColor, false);

        } else if (result instanceof AnalysisResult.Pass p) {
            addResultText("  \u2713 ", COLOR_PASS_ICON, true);
            addResultText("[" + p.guidelineName() + "] ", COLOR_PASS, true);
            addResultText("OK\n", COLOR_PASS, false);
        }
    }

    /** Show a placeholder when there are no results at all. */
    private void displayEmptyResults() {
        resultsDisplay.getChildren().clear();
        addResultText("No results to display.\n", Color.GRAY, false);
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



