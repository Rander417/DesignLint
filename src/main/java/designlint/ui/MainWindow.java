package designlint.ui;

import designlint.core.AnalysisEngine;
import designlint.core.AnalysisResult;
import designlint.core.DesignGuideline;
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
 * Phase 3: TreeView + TableView with view toggle (this version)
 *
 * === PHASE 3 ARCHITECTURE ===
 *
 * The results area is now a StackPane that holds one of three display components:
 *
 *   - TreeView: Classes as expandable nodes, violations as children, passes collapsed
 *     into a summary. Best for "what's wrong with THIS class?" workflows. Each class
 *     node shows inline severity counts and a colored left border for worst severity.
 *
 *   - TableView: Flat sortable table with columns for severity, class, guideline,
 *     and message. Best for "show me ALL warnings across ALL classes" workflows.
 *     Click column headers to sort.
 *
 * The view is controlled by toggle buttons in the filter bar. Switching views
 * re-renders from stored results without re-running analysis.
 *
 * === JAVAFX CONCEPTS FOR PHASE 3 ===
 *
 * TreeView<T>: A hierarchical list control. Each node is a TreeItem<T> which can
 * have children. TreeCells render each visible node. Unlike Swing JTree which uses
 * a TreeModel, JavaFX uses a simple parent-child TreeItem structure.
 *
 * TreeCell: The visual representation of one TreeItem. A cell factory creates these.
 * IMPORTANT: cells are RECYCLED — the same cell object renders different items as
 * you scroll. The updateItem() method must handle this by clearing state when the
 * item is null or empty, and fully rebuilding the visual for each new item.
 *
 * TableView<T>: A sortable, column-based grid. Each column has a cellValueFactory
 * (extracts data from the row object) and optionally a cellFactory (custom rendering).
 * Columns are sortable by clicking headers — JavaFX handles the sort UI automatically.
 *
 * StackPane: A layout that stacks children on top of each other. Only the visible
 * child is shown. We use this to swap between TreeView and TableView in the same
 * space without rearranging the layout.
 */
public class MainWindow {

    private final Stage stage;
    private final AnalysisEngine engine;

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

    /** Stored results from the last analysis run, used for re-filtering/sorting. */
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
    // Inner types: data wrappers for the left-panel lists and result views
    // ==================================================================================

    /** Wrapper for classes shown in the loaded-classes ListView. */
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

    /** Wrapper for guidelines in the guideline-selection ListView. */
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
    // These are the data objects stored in TreeItem<Object>. The tree cell factory
    // uses instanceof to decide how to render each one.

    /**
     * Data for a class-level tree node. Holds the class name plus pre-computed
     * severity counts so the cell factory doesn't have to recount every render.
     */
    private record ClassNodeData(
            String className,
            int errorCount,
            int warningCount,
            int advisoryCount,
            int passCount
    ) {
        /** The worst (most critical) severity present in this class's results. */
        String worstSeverityColor() {
            if (errorCount > 0) return HEX_ERROR_ICON;
            if (warningCount > 0) return HEX_WARN_ICON;
            if (advisoryCount > 0) return HEX_ADV_ICON;
            return HEX_PASS_ICON;
        }
    }

    /** Data for a violation child node in the tree. */
    private record ViolationNodeData(AnalysisResult.Violation violation) {}

    /** Data for the collapsible "N checks passed" summary node. */
    private record PassSummaryData(int count) {}

    /** Data for an individual pass detail (child of PassSummaryData). */
    private record PassDetailData(String guidelineName) {}

    // --- TableView row type ---
    /**
     * Flat row for the TableView. We pre-extract fields from AnalysisResult so that
     * TableColumn cell value factories are simple property lookups.
     */
    private record TableRowData(
            String severity,       // "ERROR", "WARNING", "ADVISORY", "PASS"
            int severityOrder,     // 0, 1, 2, 3 — for sorting
            String className,
            String guideline,
            String message
    ) {
        static TableRowData from(AnalysisResult result) {
            if (result instanceof AnalysisResult.Violation v) {
                return new TableRowData(
                        v.severity().name(),
                        v.severity().ordinal(),
                        v.className(),
                        v.guidelineName(),
                        v.message()
                );
            } else if (result instanceof AnalysisResult.Pass p) {
                return new TableRowData("PASS", 3, p.className(), p.guidelineName(), "OK");
            }
            throw new IllegalArgumentException("Unknown result type: " + result);
        }
    }

    // ==================================================================================
    // Construction and UI building
    // ==================================================================================

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
        guidelineListView.setPrefHeight(150);

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

        // === RIGHT PANEL: Results ===
        Label resultsLabel = new Label("Analysis Results:");
        resultsLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        // --- Summary banner ---
        summaryBar = new HBox(6);
        summaryBar.setAlignment(Pos.CENTER_LEFT);
        summaryBar.setPadding(new Insets(6, 10, 6, 10));
        summaryBar.getStyleClass().add("summary-bar");
        summaryBar.setVisible(false);
        summaryBar.setManaged(false);

        // --- Filter, sort, and view controls ---
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

        // View toggle — mutually exclusive via a ToggleGroup
        ToggleGroup viewGroup = new ToggleGroup();
        viewTreeToggle = new ToggleButton("Tree View");
        viewTreeToggle.getStyleClass().addAll("filter-toggle", "view-toggle");
        viewTreeToggle.setToggleGroup(viewGroup);
        viewTreeToggle.setSelected(true);
        viewTreeToggle.setOnAction(e -> {
            // Prevent deselecting the active view (ToggleGroup allows it by default)
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

        // --- Results container: StackPane holds the active view ---
        resultsTreeView = createTreeView();
        resultsTableView = createTableView();

        resultsContainer = new StackPane(resultsTreeView, resultsTableView);
        resultsTableView.setVisible(false);  // tree is default
        VBox.setVgrow(resultsContainer, Priority.ALWAYS);

        VBox rightPanel = new VBox(8,
                resultsLabel, summaryBar, filterBar, resultsContainer
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

        Scene scene = new Scene(root, 1050, 720);

        scene.getStylesheets().add(getClass().getResource("/styles.css") != null
                ? getClass().getResource("/styles.css").toExternalForm()
                : "");

        stage.setScene(scene);
        stage.setTitle("DesignLint \u2014 Java Design Guideline Analyzer");
        stage.show();
    }

    // ==================================================================================
    // TreeView setup and cell factory
    // ==================================================================================

    /**
     * Create and configure the TreeView for results display.
     *
     * The tree structure is:
     *   Root (hidden)
     *   ├── ClassNodeData("com.example.Foo", counts...)
     *   │   ├── ViolationNodeData(violation1)
     *   │   ├── ViolationNodeData(violation2)
     *   │   └── PassSummaryData(3)        ← collapsible
     *   │       ├── PassDetailData("Cloneable Check")
     *   │       ├── PassDetailData("Empty Catch Check")
     *   │       └── PassDetailData("Mutable Static Check")
     *   └── ClassNodeData("com.example.Bar", counts...)
     *       └── ...
     *
     * The cell factory uses instanceof on the TreeItem's value to decide rendering.
     */
    private TreeView<Object> createTreeView() {
        TreeView<Object> tree = new TreeView<>();
        tree.setShowRoot(false);
        tree.setRoot(new TreeItem<>());  // invisible root

        tree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);

                // CRITICAL: cells are recycled. Must clear everything for empty/null cells.
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }

                setText(null); // we use graphic exclusively for full control

                if (item instanceof ClassNodeData cn) {
                    setGraphic(buildClassNodeGraphic(cn));
                    // Left border colored by worst severity
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

    /**
     * Build the graphic for a class-level tree node.
     *
     * Layout: [package.ClassName] [spacer] [mini error badge] [mini warn badge] ...
     *
     * The package name is grayed out, class name is bold indigo. Mini badges on the
     * right show counts at a glance without expanding.
     */
    private HBox buildClassNodeGraphic(ClassNodeData cn) {
        String fullName = cn.className();
        int lastDot = fullName.lastIndexOf('.');
        String pkg = lastDot >= 0 ? fullName.substring(0, lastDot + 1) : "";
        String simpleName = lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;

        Label pkgLabel = new Label(pkg);
        pkgLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px;");

        Label nameLabel = new Label(simpleName);
        nameLabel.setStyle("-fx-text-fill: " + HEX_CLASS_HDR + "; -fx-font-size: 12px; -fx-font-weight: bold;");

        // Spacer pushes badges to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(0, pkgLabel, nameLabel, spacer);
        row.setAlignment(Pos.CENTER_LEFT);

        // Add mini severity count badges (only for non-zero counts)
        if (cn.errorCount() > 0)    row.getChildren().add(miniBadge(String.valueOf(cn.errorCount()), "mini-badge-error"));
        if (cn.warningCount() > 0)  row.getChildren().add(miniBadge(String.valueOf(cn.warningCount()), "mini-badge-warning"));
        if (cn.advisoryCount() > 0) row.getChildren().add(miniBadge(String.valueOf(cn.advisoryCount()), "mini-badge-advisory"));
        if (cn.passCount() > 0)     row.getChildren().add(miniBadge(String.valueOf(cn.passCount()), "mini-badge-pass"));

        // Give the row enough width for badges to spread out
        row.setMinWidth(400);
        row.setPrefWidth(500);

        return row;
    }

    /** Create a small colored count badge for class node headers. */
    private Label miniBadge(String text, String styleClass) {
        Label badge = new Label(text);
        badge.getStyleClass().addAll("mini-badge", styleClass);
        badge.setPadding(new Insets(0, 5, 0, 5));
        HBox.setMargin(badge, new Insets(0, 2, 0, 2));
        return badge;
    }

    /**
     * Build the graphic for a violation child node.
     * Layout: [icon] [SEVERITY tag] [guideline name] [message]
     */
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

    /** Build the graphic for the "N checks passed" summary node. */
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

    /** Build the graphic for an individual pass detail row. */
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

    /**
     * Create and configure the TableView for flat, sortable results.
     *
     * Columns: Severity | Class | Guideline | Message
     *
     * The severity column uses a custom cell factory for colored badge rendering.
     * All columns are sortable by clicking headers — JavaFX handles the sort UI.
     * The comparator for the severity column uses the numeric severityOrder so that
     * ERROR sorts before WARNING before ADVISORY before PASS.
     */
    @SuppressWarnings("unchecked")
    private TableView<TableRowData> createTableView() {
        TableView<TableRowData> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No results to display."));

        // --- Severity column ---
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
                if (empty || severity == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
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

        // --- Class column ---
        TableColumn<TableRowData, String> classCol = new TableColumn<>("Class");
        classCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().className()));
        classCol.setPrefWidth(180);
        classCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String className, boolean empty) {
                super.updateItem(className, empty);
                if (empty || className == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                // Show just the simple class name, full name in tooltip
                int lastDot = className.lastIndexOf('.');
                setText(lastDot >= 0 ? className.substring(lastDot + 1) : className);
                setTooltip(new Tooltip(className));
                setStyle("-fx-text-fill: " + HEX_CLASS_HDR + "; -fx-font-weight: bold; -fx-font-size: 12px;");
            }
        });

        // --- Guideline column ---
        TableColumn<TableRowData, String> guideCol = new TableColumn<>("Guideline");
        guideCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().guideline()));
        guideCol.setPrefWidth(160);

        // --- Message column ---
        TableColumn<TableRowData, String> msgCol = new TableColumn<>("Message");
        msgCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().message()));
        // Color the message cell based on severity
        msgCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String msg, boolean empty) {
                super.updateItem(msg, empty);
                if (empty || msg == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(msg);
                // Get the row's severity to color the text
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

        File selectedDir = dirChooser.showDialog(stage);
        if (selectedDir == null) return;

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
    // Results rendering — filtering, sorting, and view population
    // ==================================================================================

    /**
     * Re-render results using stored data, current filters, sort, and view mode.
     *
     * Flow: filter → sort → populate active view → update summary & status
     */
    private void refreshResults() {
        if (lastResults.isEmpty()) {
            resultsTreeView.getRoot().getChildren().clear();
            resultsTableView.getItems().clear();
            statusBar.setText("No results to display.");
            return;
        }

        // Always update summary with unfiltered counts
        updateSummary(lastResults);

        // Filter
        List<AnalysisResult> filtered = lastResults.stream()
                .filter(this::passesFilter)
                .collect(Collectors.toList());

        // Sort
        List<AnalysisResult> sorted;
        if (sortBySeverity.isSelected()) {
            sorted = sortBySeverityThenClass(filtered);
        } else {
            sorted = sortByClassThenSeverity(filtered);
        }

        // Populate the active view
        boolean isTreeMode = viewTreeToggle.isSelected();
        resultsTreeView.setVisible(isTreeMode);
        resultsTableView.setVisible(!isTreeMode);

        if (isTreeMode) {
            populateTreeView(sorted);
        } else {
            populateTableView(sorted);
        }

        // Status bar
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

    /**
     * Populate the TreeView from sorted/filtered results.
     *
     * Groups results by class name. Within each class:
     *   - Violations appear as individual child nodes (severity order)
     *   - Passes collapse into a single "N checks passed" node (expandable)
     *
     * Class nodes auto-expand if they have violations, stay collapsed if all passed.
     * When "Sort by Severity" is active, classes with errors sort before classes
     * with only warnings, etc.
     */
    private void populateTreeView(List<AnalysisResult> results) {
        TreeItem<Object> root = resultsTreeView.getRoot();
        root.getChildren().clear();

        // Group by class, preserving sort order
        Map<String, List<AnalysisResult>> byClass = results.stream()
                .collect(Collectors.groupingBy(AnalysisResult::className,
                        LinkedHashMap::new, Collectors.toList()));

        for (var entry : byClass.entrySet()) {
            String className = entry.getKey();
            List<AnalysisResult> classResults = entry.getValue();

            // Separate violations and passes
            List<AnalysisResult.Violation> violations = classResults.stream()
                    .filter(r -> r instanceof AnalysisResult.Violation)
                    .map(r -> (AnalysisResult.Violation) r)
                    .collect(Collectors.toList());

            List<AnalysisResult.Pass> passes = classResults.stream()
                    .filter(r -> r instanceof AnalysisResult.Pass)
                    .map(r -> (AnalysisResult.Pass) r)
                    .collect(Collectors.toList());

            // Compute severity counts for the class header badges
            int errors = (int) violations.stream().filter(v -> v.severity() == Severity.ERROR).count();
            int warnings = (int) violations.stream().filter(v -> v.severity() == Severity.WARNING).count();
            int advisories = (int) violations.stream().filter(v -> v.severity() == Severity.ADVISORY).count();

            ClassNodeData classData = new ClassNodeData(className, errors, warnings, advisories, passes.size());
            TreeItem<Object> classItem = new TreeItem<>(classData);
            classItem.setExpanded(!violations.isEmpty());  // auto-expand if there are problems

            // Add violation children
            for (AnalysisResult.Violation v : violations) {
                classItem.getChildren().add(new TreeItem<>(new ViolationNodeData(v)));
            }

            // Add collapsed pass summary (with expandable details)
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

    /**
     * Populate the TableView from sorted/filtered results.
     * Converts each AnalysisResult into a flat TableRowData for column display.
     */
    private void populateTableView(List<AnalysisResult> results) {
        ObservableList<TableRowData> rows = FXCollections.observableArrayList(
                results.stream()
                        .map(TableRowData::from)
                        .collect(Collectors.toList())
        );
        resultsTableView.setItems(rows);

        // If sort-by-severity is active, apply initial sort on the severity column
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
                .sorted(Comparator
                        .comparingInt(this::severityOrdinal)
                        .thenComparing(AnalysisResult::className)
                        .thenComparing(AnalysisResult::guidelineName))
                .collect(Collectors.toList());
    }

    private List<AnalysisResult> sortByClassThenSeverity(List<AnalysisResult> results) {
        return results.stream()
                .sorted(Comparator
                        .comparing(AnalysisResult::className)
                        .thenComparingInt(this::severityOrdinal)
                        .thenComparing(AnalysisResult::guidelineName))
                .collect(Collectors.toList());
    }

    private int severityOrdinal(AnalysisResult result) {
        if (result instanceof AnalysisResult.Violation v) {
            return v.severity().ordinal();
        }
        return 3;
    }

    // ==================================================================================
    // Summary banner
    // ==================================================================================

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

    private String plural(long count) {
        return count == 1 ? "" : "s";
    }

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