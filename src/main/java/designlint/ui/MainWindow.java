package designlint.ui;

import designlint.core.AnalysisEngine;
import designlint.core.AnalysisResult;
import designlint.core.ConfigService;
import designlint.core.DesignGuideline;
import designlint.core.DesignLintConfig;
import designlint.core.Severity;
import sootup.core.model.SootClass;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The main window for DesignLint.
 *
 * === EVOLUTION ===
 *
 * Phase 1: Severity-aware color coding and summary banner
 * Phase 2: Filter toggles and sort controls
 * Phase 3: TreeView + TableView with view toggle
 * Phase 4: Configuration persistence with settings dialog
 *
 * === CONFIGURATION (Phase 4) ===
 *
 * On startup, the ConfigService loads saved configuration and applies it:
 *   - Guideline enable/disable states restore from config
 *   - Filter toggles, view mode, and sort preference restore from config
 *   - Window size and position restore from config
 *   - File chooser opens to last-used classpath directory
 *
 * On close, session state auto-saves to the global config:
 *   - Current window size and position
 *   - Last-used classpath directory
 *   - Current filter, view, and sort state
 *   - Current guideline selections
 *
 * The Settings dialog (gear icon in toolbar) provides intentional control over
 * defaults. "Save as Defaults" writes to global config, "Save for This Project"
 * writes a sparse project config next to the analyzed classes.
 *
 * === JAVAFX CONCEPTS ===
 *
 * Stage.setOnCloseRequest: Fires when the user closes the window (X button, Alt+F4).
 * We hook this to auto-save session state. The event can be consumed to prevent
 * closing (e.g., "unsaved changes" prompts), but we just save and let it close.
 *
 * StackPane: Stacks children on top of each other; we toggle visibility to swap
 * between TreeView and TableView in the results area.
 *
 * TreeView cell recycling: Cells are reused as you scroll. The updateItem() method
 * MUST clear all state for empty/null items to avoid visual artifacts.
 */
public class MainWindow {

    private final Stage stage;
    private final AnalysisEngine engine;
    private final ConfigService configService;

    // --- UI Components ---
    private ListView<ClassItem> classListView;
    private ListView<GuidelineItem> guidelineListView;
    private Label statusBar;
    private Button loadButton;
    private Button analyzeButton;

    // --- Results display: summary, filter, sort, and view controls ---
    private HBox summaryBar;
    private ToggleButton filterError;
    private ToggleButton filterWarning;
    private ToggleButton filterAdvisory;
    private ToggleButton filterPasses;
    private ToggleButton sortBySeverity;
    private ToggleButton viewTreeToggle;
    private ToggleButton viewTableToggle;

    // --- Results area: swappable views in a StackPane ---
    private StackPane resultsContainer;
    private TreeView<Object> resultsTreeView;
    private TableView<TableRowData> resultsTableView;

    /** Stored results from the last analysis run. */
    private List<AnalysisResult> lastResults = List.of();

    // --- Severity color palette ---
    private static final String HEX_ERROR      = "#c62828";
    private static final String HEX_ERROR_ICON = "#d32f2f";
    private static final String HEX_WARN       = "#e65100";
    private static final String HEX_WARN_ICON  = "#f57c00";
    private static final String HEX_ADV        = "#1565c0";
    private static final String HEX_ADV_ICON   = "#1976d2";
    private static final String HEX_PASS       = "#2e7d32";
    private static final String HEX_PASS_ICON  = "#388e3c";
    private static final String HEX_CLASS_HDR  = "#1a237e";

    // ==================================================================================
    // Inner types
    // ==================================================================================

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

    // --- TreeView node types ---
    private record ClassNodeData(String className, int errorCount, int warningCount,
                                 int advisoryCount, int passCount) {
        String worstSeverityColor() {
            if (errorCount > 0) return HEX_ERROR_ICON;
            if (warningCount > 0) return HEX_WARN_ICON;
            if (advisoryCount > 0) return HEX_ADV_ICON;
            return HEX_PASS_ICON;
        }
    }
    private record ViolationNodeData(AnalysisResult.Violation violation) {}
    private record PassSummaryData(int count) {}
    private record PassDetailData(String guidelineName) {}

    // --- TableView row type ---
    private record TableRowData(String severity, int severityOrder, String className,
                                String guideline, String message) {
        static TableRowData from(AnalysisResult result) {
            if (result instanceof AnalysisResult.Violation v) {
                return new TableRowData(v.severity().name(), v.severity().ordinal(),
                        v.className(), v.guidelineName(), v.message());
            } else if (result instanceof AnalysisResult.Pass p) {
                return new TableRowData("PASS", 3, p.className(), p.guidelineName(), "OK");
            }
            throw new IllegalArgumentException("Unknown result type: " + result);
        }
    }

    // ==================================================================================
    // Construction
    // ==================================================================================

    public MainWindow(Stage stage, AnalysisEngine engine, ConfigService configService) {
        this.stage = stage;
        this.engine = engine;
        this.configService = configService;
        buildUI();
        applyConfig();

        // Auto-save session state when the window closes
        stage.setOnCloseRequest(this::handleWindowClose);
    }

    private void buildUI() {
        // === TOP: Toolbar ===
        loadButton = new Button("Load Classes...");
        loadButton.setOnAction(e -> handleLoad());

        analyzeButton = new Button("Run Analysis");
        analyzeButton.setDisable(true);
        analyzeButton.setOnAction(e -> handleAnalyze());

        Button selectAllClasses = new Button("Select All");
        selectAllClasses.setOnAction(e -> classListView.getItems().forEach(i -> i.selected.set(true)));

        Button deselectAllClasses = new Button("Deselect All");
        deselectAllClasses.setOnAction(e -> classListView.getItems().forEach(i -> i.selected.set(false)));

        // Settings button (gear icon via Unicode)
        Button settingsButton = new Button("\u2699 Settings");
        settingsButton.setOnAction(e -> handleSettings());

        ToolBar toolBar = new ToolBar(
                loadButton,
                new Separator(),
                selectAllClasses,
                deselectAllClasses,
                new Separator(),
                analyzeButton,
                new Separator(),
                settingsButton
        );

        // === LEFT PANEL ===

        Label classLabel = new Label("Loaded Classes:");
        classLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        classListView = new ListView<>();
        classListView.setCellFactory(CheckBoxListCell.forListView(item -> item.selected));
        classListView.setPrefHeight(300);

        Label guidelineLabel = new Label("Design Guidelines:");
        guidelineLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        guidelineListView = new ListView<>();
        guidelineListView.setPrefHeight(150);

        ObservableList<GuidelineItem> guidelineItems = FXCollections.observableArrayList(
                engine.getAvailableGuidelines().stream()
                        .map(GuidelineItem::new)
                        .collect(Collectors.toList())
        );
        guidelineListView.setItems(guidelineItems);

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

        // === RIGHT PANEL ===
        Label resultsLabel = new Label("Analysis Results:");
        resultsLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        summaryBar = new HBox(6);
        summaryBar.setAlignment(Pos.CENTER_LEFT);
        summaryBar.setPadding(new Insets(6, 10, 6, 10));
        summaryBar.getStyleClass().add("summary-bar");
        summaryBar.setVisible(false);
        summaryBar.setManaged(false);

        // Filter, sort, and view controls
        filterError = createFilterToggle("Errors", "filter-error");
        filterWarning = createFilterToggle("Warnings", "filter-warning");
        filterAdvisory = createFilterToggle("Advisories", "filter-advisory");
        filterPasses = createFilterToggle("Passes", "filter-pass");

        sortBySeverity = new ToggleButton("Sort by Severity");
        sortBySeverity.getStyleClass().addAll("filter-toggle", "sort-toggle");
        sortBySeverity.setSelected(false);
        sortBySeverity.setTooltip(new Tooltip(
                "Tree: most-problematic classes first\nTable: severity column sort"));
        sortBySeverity.setOnAction(e -> refreshResults());

        ToggleGroup viewGroup = new ToggleGroup();
        viewTreeToggle = new ToggleButton("Tree View");
        viewTreeToggle.getStyleClass().addAll("filter-toggle", "view-toggle");
        viewTreeToggle.setToggleGroup(viewGroup);
        viewTreeToggle.setSelected(true);
        viewTreeToggle.setOnAction(e -> {
            if (!viewTreeToggle.isSelected()) viewTreeToggle.setSelected(true);
            else refreshResults();
        });

        viewTableToggle = new ToggleButton("Table View");
        viewTableToggle.getStyleClass().addAll("filter-toggle", "view-toggle");
        viewTableToggle.setToggleGroup(viewGroup);
        viewTableToggle.setOnAction(e -> {
            if (!viewTableToggle.isSelected()) viewTableToggle.setSelected(true);
            else refreshResults();
        });

        Label showLabel = new Label("Show:");
        showLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        Label viewLabel = new Label("View:");
        viewLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");

        HBox filterBar = new HBox(6,
                showLabel, filterError, filterWarning, filterAdvisory, filterPasses,
                new Separator(Orientation.VERTICAL),
                sortBySeverity,
                new Separator(Orientation.VERTICAL),
                viewLabel, viewTreeToggle, viewTableToggle
        );
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(4, 8, 4, 8));
        filterBar.getStyleClass().add("filter-bar");

        resultsTreeView = createTreeView();
        resultsTableView = createTableView();

        resultsContainer = new StackPane(resultsTreeView, resultsTableView);
        resultsTableView.setVisible(false);
        VBox.setVgrow(resultsContainer, Priority.ALWAYS);

        VBox rightPanel = new VBox(8,
                resultsLabel, summaryBar, filterBar, resultsContainer
        );
        rightPanel.setPadding(new Insets(8));

        // === SPLIT PANE ===
        SplitPane splitPane = new SplitPane(leftPanel, rightPanel);
        splitPane.setDividerPositions(0.35);

        // === STATUS BAR ===
        statusBar = new Label("Ready. Load classes to begin analysis.");
        statusBar.setPadding(new Insets(4, 8, 4, 8));
        statusBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc; " +
                           "-fx-border-width: 1 0 0 0;");

        // === ROOT LAYOUT ===
        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(splitPane);
        root.setBottom(statusBar);

        // Window size comes from config (applied in applyConfig), but set defaults here
        Scene scene = new Scene(root, 1050, 720);

        scene.getStylesheets().add(getClass().getResource("/styles.css") != null
                ? getClass().getResource("/styles.css").toExternalForm()
                : "");

        stage.setScene(scene);
        stage.setTitle("DesignLint \u2014 Java Design Guideline Analyzer");
        stage.show();
    }

    // ==================================================================================
    // Configuration: apply on startup, save on close
    // ==================================================================================

    /**
     * Apply saved configuration to the UI.
     * Called once after buildUI(), restoring the previous session's state.
     */
    private void applyConfig() {
        DesignLintConfig config = configService.getConfig();

        // Guideline enable/disable defaults
        for (GuidelineItem item : guidelineListView.getItems()) {
            item.selected.set(config.isGuidelineEnabled(item.guideline.name()));
        }

        // Filter toggles
        filterError.setSelected(config.getFilters().isShowErrors());
        filterWarning.setSelected(config.getFilters().isShowWarnings());
        filterAdvisory.setSelected(config.getFilters().isShowAdvisories());
        filterPasses.setSelected(config.getFilters().isShowPasses());

        // View mode
        if ("table".equals(config.getViewMode())) {
            viewTableToggle.setSelected(true);
            resultsTreeView.setVisible(false);
            resultsTableView.setVisible(true);
        } else {
            viewTreeToggle.setSelected(true);
            resultsTreeView.setVisible(true);
            resultsTableView.setVisible(false);
        }

        // Sort preference
        sortBySeverity.setSelected(config.isSortBySeverity());

        // Window size and position
        DesignLintConfig.WindowConfig wc = config.getWindow();
        if (wc.getWidth() > 0) stage.setWidth(wc.getWidth());
        if (wc.getHeight() > 0) stage.setHeight(wc.getHeight());
        if (wc.getX() >= 0) stage.setX(wc.getX());
        if (wc.getY() >= 0) stage.setY(wc.getY());
    }

    /**
     * Capture current UI state into the config and save.
     * Called automatically when the window closes.
     */
    private void handleWindowClose(WindowEvent event) {
        DesignLintConfig config = configService.getConfig();

        // Guideline states
        Map<String, Boolean> guidelineState = new LinkedHashMap<>();
        for (GuidelineItem item : guidelineListView.getItems()) {
            guidelineState.put(item.guideline.name(), item.selected.get());
        }
        config.setGuidelines(guidelineState);

        // Filters
        config.getFilters().setShowErrors(filterError.isSelected());
        config.getFilters().setShowWarnings(filterWarning.isSelected());
        config.getFilters().setShowAdvisories(filterAdvisory.isSelected());
        config.getFilters().setShowPasses(filterPasses.isSelected());

        // View and sort
        config.setViewMode(viewTableToggle.isSelected() ? "table" : "tree");
        config.setSortBySeverity(sortBySeverity.isSelected());

        // Window geometry
        DesignLintConfig.WindowConfig wc = config.getWindow();
        wc.setWidth(stage.getWidth());
        wc.setHeight(stage.getHeight());
        wc.setX(stage.getX());
        wc.setY(stage.getY());

        configService.saveGlobal();
    }

    /**
     * Open the Settings dialog. If the user saves, re-apply the config to the UI.
     */
    private void handleSettings() {
        SettingsDialog dialog = new SettingsDialog(configService, engine.getAvailableGuidelines());
        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && SettingsDialog.wasSaved(result.get())) {
            // Re-apply config to UI (guideline checkboxes, filter toggles, view mode)
            applyConfig();

            // If there are existing results, re-render with new filter/sort settings
            if (!lastResults.isEmpty()) {
                refreshResults();
            }

            statusBar.setText("Settings saved.");
        }
    }

    // ==================================================================================
    // TreeView setup and cell factory
    // ==================================================================================

    private TreeView<Object> createTreeView() {
        TreeView<Object> tree = new TreeView<>();
        tree.setShowRoot(false);
        tree.setRoot(new TreeItem<>());

        tree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }
                setText(null);

                if (item instanceof ClassNodeData cn) {
                    setGraphic(buildClassNodeGraphic(cn));
                    setStyle("-fx-border-color: transparent transparent transparent " +
                             cn.worstSeverityColor() + ";" +
                             "-fx-border-width: 0 0 0 3; -fx-padding: 4 8 4 6;");
                } else if (item instanceof ViolationNodeData vn) {
                    setGraphic(buildViolationGraphic(vn.violation()));
                    setStyle("-fx-padding: 2 8 2 4;");
                } else if (item instanceof PassSummaryData ps) {
                    setGraphic(buildPassSummaryGraphic(ps));
                    setStyle("-fx-padding: 2 8 2 4;");
                } else if (item instanceof PassDetailData pd) {
                    setGraphic(buildPassDetailGraphic(pd));
                    setStyle("-fx-padding: 1 8 1 4;");
                } else {
                    setGraphic(null);
                    setStyle("");
                }
            }
        });
        return tree;
    }

    private HBox buildClassNodeGraphic(ClassNodeData cn) {
        String fullName = cn.className();
        int lastDot = fullName.lastIndexOf('.');
        String pkg = lastDot >= 0 ? fullName.substring(0, lastDot + 1) : "";
        String simpleName = lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;

        Label pkgLabel = new Label(pkg);
        pkgLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px;");

        Label nameLabel = new Label(simpleName);
        nameLabel.setStyle("-fx-text-fill: " + HEX_CLASS_HDR + "; -fx-font-size: 12px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(0, pkgLabel, nameLabel, spacer);
        row.setAlignment(Pos.CENTER_LEFT);

        if (cn.errorCount() > 0)    row.getChildren().add(miniBadge(String.valueOf(cn.errorCount()), "mini-badge-error"));
        if (cn.warningCount() > 0)  row.getChildren().add(miniBadge(String.valueOf(cn.warningCount()), "mini-badge-warning"));
        if (cn.advisoryCount() > 0) row.getChildren().add(miniBadge(String.valueOf(cn.advisoryCount()), "mini-badge-advisory"));
        if (cn.passCount() > 0)     row.getChildren().add(miniBadge(String.valueOf(cn.passCount()), "mini-badge-pass"));

        row.setMinWidth(400);
        row.setPrefWidth(500);
        return row;
    }

    private Label miniBadge(String text, String styleClass) {
        Label badge = new Label(text);
        badge.getStyleClass().addAll("mini-badge", styleClass);
        badge.setPadding(new Insets(0, 5, 0, 5));
        HBox.setMargin(badge, new Insets(0, 2, 0, 2));
        return badge;
    }

    private HBox buildViolationGraphic(AnalysisResult.Violation v) {
        String icon, sevTag, hexIcon, hexText, bgHex, borderHex;
        switch (v.severity()) {
            case ERROR -> {
                icon = "\u2717"; sevTag = "ERROR";
                hexIcon = HEX_ERROR_ICON; hexText = HEX_ERROR;
                bgHex = "#ffebee"; borderHex = "#ef9a9a";
            }
            case WARNING -> {
                icon = "\u26a0"; sevTag = "WARNING";
                hexIcon = HEX_WARN_ICON; hexText = HEX_WARN;
                bgHex = "#fff3e0"; borderHex = "#ffcc80";
            }
            case ADVISORY -> {
                icon = "\u2139"; sevTag = "ADVISORY";
                hexIcon = HEX_ADV_ICON; hexText = HEX_ADV;
                bgHex = "#e3f2fd"; borderHex = "#90caf9";
            }
            default -> {
                icon = "\u2717"; sevTag = "ERROR";
                hexIcon = HEX_ERROR_ICON; hexText = HEX_ERROR;
                bgHex = "#ffebee"; borderHex = "#ef9a9a";
            }
        }

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-text-fill: " + hexIcon + "; -fx-font-weight: bold; -fx-font-size: 12px;");
        iconLabel.setMinWidth(16);

        Label sevLabel = new Label(sevTag);
        sevLabel.setStyle("-fx-text-fill: " + hexText + "; -fx-font-weight: bold; -fx-font-size: 10px;" +
                         "-fx-background-color: " + bgHex + "; -fx-background-radius: 3;" +
                         "-fx-border-color: " + borderHex + "; -fx-border-radius: 3; -fx-border-width: 1;" +
                         "-fx-padding: 0 4 0 4;");

        Label guideLabel = new Label(v.guidelineName());
        guideLabel.setStyle("-fx-text-fill: " + hexText + "; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label msgLabel = new Label(v.message());
        msgLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 12px;");
        msgLabel.setWrapText(true);

        HBox row = new HBox(6, iconLabel, sevLabel, guideLabel, msgLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildPassSummaryGraphic(PassSummaryData ps) {
        Label icon = new Label("\u2713");
        icon.setStyle("-fx-text-fill: " + HEX_PASS_ICON + "; -fx-font-weight: bold; -fx-font-size: 12px;");
        icon.setMinWidth(16);

        String text = ps.count() + " check" + (ps.count() == 1 ? "" : "s") + " passed";
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + HEX_PASS + "; -fx-font-weight: bold; -fx-font-size: 12px;");

        HBox row = new HBox(6, icon, label);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildPassDetailGraphic(PassDetailData pd) {
        Label icon = new Label("\u2713");
        icon.setStyle("-fx-text-fill: " + HEX_PASS_ICON + "; -fx-font-size: 11px;");
        icon.setMinWidth(14);

        Label label = new Label(pd.guidelineName());
        label.setStyle("-fx-text-fill: #66bb6a; -fx-font-size: 11px;");

        HBox row = new HBox(4, icon, label);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ==================================================================================
    // TableView setup
    // ==================================================================================

    @SuppressWarnings("unchecked")
    private TableView<TableRowData> createTableView() {
        TableView<TableRowData> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No results to display."));

        // Severity column
        TableColumn<TableRowData, String> sevCol = new TableColumn<>("Severity");
        sevCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().severity()));
        sevCol.setComparator(Comparator.comparingInt(s -> {
            return switch (s) {
                case "ERROR" -> 0; case "WARNING" -> 1; case "ADVISORY" -> 2; default -> 3;
            };
        }));
        sevCol.setPrefWidth(90);
        sevCol.setMinWidth(80);
        sevCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String severity, boolean empty) {
                super.updateItem(severity, empty);
                if (empty || severity == null) { setGraphic(null); setText(null); return; }
                String icon, hex, bgHex, borderHex;
                switch (severity) {
                    case "ERROR"    -> { icon = "\u2717"; hex = HEX_ERROR;  bgHex = "#ffebee"; borderHex = "#ef9a9a"; }
                    case "WARNING"  -> { icon = "\u26a0"; hex = HEX_WARN;   bgHex = "#fff3e0"; borderHex = "#ffcc80"; }
                    case "ADVISORY" -> { icon = "\u2139"; hex = HEX_ADV;    bgHex = "#e3f2fd"; borderHex = "#90caf9"; }
                    default         -> { icon = "\u2713"; hex = HEX_PASS;   bgHex = "#e8f5e9"; borderHex = "#a5d6a7"; }
                }
                Label badge = new Label(icon + " " + severity);
                badge.setStyle("-fx-text-fill: " + hex + "; -fx-font-weight: bold; -fx-font-size: 10px;" +
                              "-fx-background-color: " + bgHex + "; -fx-background-radius: 3;" +
                              "-fx-border-color: " + borderHex + "; -fx-border-radius: 3; -fx-border-width: 1;" +
                              "-fx-padding: 1 5 1 5;");
                setGraphic(badge);
                setText(null);
            }
        });

        // Class column
        TableColumn<TableRowData, String> classCol = new TableColumn<>("Class");
        classCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().className()));
        classCol.setPrefWidth(180);
        classCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String className, boolean empty) {
                super.updateItem(className, empty);
                if (empty || className == null) { setText(null); setStyle(""); return; }
                int lastDot = className.lastIndexOf('.');
                setText(lastDot >= 0 ? className.substring(lastDot + 1) : className);
                setTooltip(new Tooltip(className));
                setStyle("-fx-text-fill: " + HEX_CLASS_HDR + "; -fx-font-weight: bold; -fx-font-size: 12px;");
            }
        });

        // Guideline column
        TableColumn<TableRowData, String> guideCol = new TableColumn<>("Guideline");
        guideCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().guideline()));
        guideCol.setPrefWidth(160);

        // Message column
        TableColumn<TableRowData, String> msgCol = new TableColumn<>("Message");
        msgCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().message()));
        msgCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String msg, boolean empty) {
                super.updateItem(msg, empty);
                if (empty || msg == null) { setText(null); setStyle(""); return; }
                setText(msg);
                TableRowData row = getTableRow() != null ? getTableRow().getItem() : null;
                if (row != null && "PASS".equals(row.severity())) {
                    setStyle("-fx-text-fill: #66bb6a; -fx-font-size: 12px;");
                } else {
                    setStyle("-fx-text-fill: #555555; -fx-font-size: 12px;");
                }
            }
        });

        table.getColumns().addAll(sevCol, classCol, guideCol, msgCol);
        return table;
    }

    // ==================================================================================
    // Filter toggle factory
    // ==================================================================================

    private ToggleButton createFilterToggle(String label, String styleClass) {
        ToggleButton toggle = new ToggleButton(label);
        toggle.setSelected(true);
        toggle.getStyleClass().addAll("filter-toggle", styleClass);
        toggle.setOnAction(e -> refreshResults());
        return toggle;
    }

    // ==================================================================================
    // Event handlers: Load and Analyze
    // ==================================================================================

    private void handleLoad() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Directory Containing .class Files");

        // Start in the last-used directory if available
        String lastPath = configService.getConfig().getLastClasspath();
        if (lastPath != null) {
            File lastDir = new File(lastPath);
            if (lastDir.isDirectory()) {
                dirChooser.setInitialDirectory(lastDir);
            }
        }

        File selectedDir = dirChooser.showDialog(stage);
        if (selectedDir == null) return;

        statusBar.setText("Loading classes from: " + selectedDir.getAbsolutePath() + "...");

        try {
            engine.getClassLoadingService().load(selectedDir.toPath(), Set.of());

            // Save last classpath and reload config (picks up project config if present)
            configService.getConfig().setLastClasspath(selectedDir.getAbsolutePath());
            configService.load(selectedDir.toPath());
            applyConfig();  // project config may change guideline/filter defaults

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
            String projectNote = configService.hasProjectDir() &&
                    java.nio.file.Files.exists(selectedDir.toPath().resolve("designlint.json"))
                    ? " (project config loaded)" : "";
            statusBar.setText("Loaded " + count + " class(es)." + projectNote +
                    " Select classes and guidelines, then run analysis.");

        } catch (Exception ex) {
            showError("Failed to load classes", ex.getMessage());
            statusBar.setText("Error loading classes. See error dialog.");
        }
    }

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
    // Results rendering
    // ==================================================================================

    private void refreshResults() {
        if (lastResults.isEmpty()) {
            resultsTreeView.getRoot().getChildren().clear();
            resultsTableView.getItems().clear();
            statusBar.setText("No results to display.");
            return;
        }

        updateSummary(lastResults);

        List<AnalysisResult> filtered = lastResults.stream()
                .filter(this::passesFilter)
                .collect(Collectors.toList());

        List<AnalysisResult> sorted;
        if (sortBySeverity.isSelected()) {
            sorted = sortBySeverityThenClass(filtered);
        } else {
            sorted = sortByClassThenSeverity(filtered);
        }

        boolean isTreeMode = viewTreeToggle.isSelected();
        resultsTreeView.setVisible(isTreeMode);
        resultsTableView.setVisible(!isTreeMode);

        if (isTreeMode) {
            populateTreeView(sorted);
        } else {
            populateTableView(sorted);
        }

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

    private void populateTreeView(List<AnalysisResult> results) {
        TreeItem<Object> root = resultsTreeView.getRoot();
        root.getChildren().clear();

        Map<String, List<AnalysisResult>> byClass = results.stream()
                .collect(Collectors.groupingBy(AnalysisResult::className,
                        LinkedHashMap::new, Collectors.toList()));

        for (var entry : byClass.entrySet()) {
            String className = entry.getKey();
            List<AnalysisResult> classResults = entry.getValue();

            List<AnalysisResult.Violation> violations = classResults.stream()
                    .filter(r -> r instanceof AnalysisResult.Violation)
                    .map(r -> (AnalysisResult.Violation) r)
                    .collect(Collectors.toList());

            List<AnalysisResult.Pass> passes = classResults.stream()
                    .filter(r -> r instanceof AnalysisResult.Pass)
                    .map(r -> (AnalysisResult.Pass) r)
                    .collect(Collectors.toList());

            int errors = (int) violations.stream().filter(v -> v.severity() == Severity.ERROR).count();
            int warnings = (int) violations.stream().filter(v -> v.severity() == Severity.WARNING).count();
            int advisories = (int) violations.stream().filter(v -> v.severity() == Severity.ADVISORY).count();

            ClassNodeData classData = new ClassNodeData(className, errors, warnings, advisories, passes.size());
            TreeItem<Object> classItem = new TreeItem<>(classData);
            classItem.setExpanded(!violations.isEmpty());

            for (AnalysisResult.Violation v : violations) {
                classItem.getChildren().add(new TreeItem<>(new ViolationNodeData(v)));
            }

            if (!passes.isEmpty()) {
                TreeItem<Object> passSummary = new TreeItem<>(new PassSummaryData(passes.size()));
                for (AnalysisResult.Pass p : passes) {
                    passSummary.getChildren().add(new TreeItem<>(new PassDetailData(p.guidelineName())));
                }
                classItem.getChildren().add(passSummary);
            }

            root.getChildren().add(classItem);
        }
    }

    private void populateTableView(List<AnalysisResult> results) {
        ObservableList<TableRowData> rows = FXCollections.observableArrayList(
                results.stream().map(TableRowData::from).collect(Collectors.toList())
        );
        resultsTableView.setItems(rows);

        if (sortBySeverity.isSelected() && !resultsTableView.getColumns().isEmpty()) {
            TableColumn<TableRowData, ?> sevCol = resultsTableView.getColumns().get(0);
            sevCol.setSortType(TableColumn.SortType.ASCENDING);
            resultsTableView.getSortOrder().clear();
            resultsTableView.getSortOrder().add(sevCol);
            resultsTableView.sort();
        }
    }

    // ==================================================================================
    // Filtering and sorting
    // ==================================================================================

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

    private List<AnalysisResult> sortBySeverityThenClass(List<AnalysisResult> results) {
        return results.stream()
                .sorted(Comparator.comparingInt(this::severityOrdinal)
                        .thenComparing(AnalysisResult::className)
                        .thenComparing(AnalysisResult::guidelineName))
                .collect(Collectors.toList());
    }

    private List<AnalysisResult> sortByClassThenSeverity(List<AnalysisResult> results) {
        return results.stream()
                .sorted(Comparator.comparing(AnalysisResult::className)
                        .thenComparingInt(this::severityOrdinal)
                        .thenComparing(AnalysisResult::guidelineName))
                .collect(Collectors.toList());
    }

    private int severityOrdinal(AnalysisResult result) {
        if (result instanceof AnalysisResult.Violation v) return v.severity().ordinal();
        return 3;
    }

    // ==================================================================================
    // Summary banner
    // ==================================================================================

    private void updateSummary(List<AnalysisResult> allResults) {
        long errors = allResults.stream()
                .filter(r -> r instanceof AnalysisResult.Violation v && v.severity() == Severity.ERROR).count();
        long warnings = allResults.stream()
                .filter(r -> r instanceof AnalysisResult.Violation v && v.severity() == Severity.WARNING).count();
        long advisories = allResults.stream()
                .filter(r -> r instanceof AnalysisResult.Violation v && v.severity() == Severity.ADVISORY).count();
        long passes = allResults.stream().filter(AnalysisResult::passed).count();

        summaryBar.getChildren().clear();

        if (errors > 0)
            summaryBar.getChildren().add(createBadge("\u25cf " + errors + " Error" + plural(errors), "badge-error"));
        if (warnings > 0)
            summaryBar.getChildren().add(createBadge("\u25cf " + warnings + " Warning" + plural(warnings), "badge-warning"));
        if (advisories > 0) {
            String advText = advisories == 1 ? "Advisory" : "Advisories";
            summaryBar.getChildren().add(createBadge("\u25cf " + advisories + " " + advText, "badge-advisory"));
        }
        if (passes > 0)
            summaryBar.getChildren().add(createBadge("\u2713 " + passes + " Passed", "badge-pass"));

        summaryBar.setVisible(true);
        summaryBar.setManaged(true);
    }

    private Label createBadge(String text, String styleClass) {
        Label badge = new Label(text);
        badge.getStyleClass().addAll("summary-badge", styleClass);
        badge.setPadding(new Insets(2, 8, 2, 8));
        return badge;
    }

    private String plural(long count) { return count == 1 ? "" : "s"; }

    // ==================================================================================
    // Utilities
    // ==================================================================================

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
