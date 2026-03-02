package designlint.ui;

import designlint.core.ConfigService;
import designlint.core.DesignGuideline;
import designlint.core.DesignLintConfig;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Settings dialog for editing DesignLint configuration defaults.
 *
 * === WHAT THIS DIALOG CONTROLS ===
 *
 * This is the "intentional defaults" editor — changes here affect future sessions:
 *
 *   - Guideline defaults: which checks are enabled by default when DesignLint starts
 *   - Filter defaults: which severity levels are visible by default
 *   - View preferences: default view mode (tree/table) and sort order
 *
 * Session state like window position and last classpath are auto-saved on close
 * and don't appear here (they're transparent to the user).
 *
 * === SAVE OPTIONS ===
 *
 *   "Save as Defaults"      → writes to ~/.designlint/config.json (global)
 *   "Save for This Project" → writes to designlint.json next to analyzed classes
 *                              (only enabled when classes have been loaded)
 *
 * The project config is sparse — it only contains the settings that differ from
 * global. This means a team can check a designlint.json into their repo to enforce
 * a shared analysis configuration.
 *
 * === JAVAFX DIALOG CONCEPTS ===
 *
 * Dialog<R>: A modal window that returns a result of type R when closed.
 * We use Dialog<ButtonType> here — the result tells us which button was pressed.
 *
 * Modality.APPLICATION_MODAL: blocks all other windows in the app while open.
 * This prevents the user from clicking "Run Analysis" while editing settings.
 *
 * DialogPane: The content area of a Dialog. Has header, content, and button areas.
 * We build our settings UI in the content area.
 */
public class SettingsDialog extends Dialog<ButtonType> {

    private final ConfigService configService;
    private final List<DesignGuideline> guidelines;
    private final DesignLintConfig config;

    // --- UI components ---
    private final Map<String, SimpleBooleanProperty> guidelineToggles = new LinkedHashMap<>();
    private CheckBox showErrors;
    private CheckBox showWarnings;
    private CheckBox showAdvisories;
    private CheckBox showPasses;
    private RadioButton viewTree;
    private RadioButton viewTable;
    private CheckBox sortBySev;

    /** Custom ButtonType for "Save for This Project". */
    private static final ButtonType SAVE_PROJECT = new ButtonType("Save for This Project",
            ButtonBar.ButtonData.LEFT);

    public SettingsDialog(ConfigService configService, List<DesignGuideline> guidelines) {
        this.configService = configService;
        this.guidelines = guidelines;
        this.config = configService.getConfig();

        setTitle("DesignLint Settings");
        initModality(Modality.APPLICATION_MODAL);
        setResizable(true);

        buildContent();
        buildButtons();
        applyCurrentConfig();

        // Handle button actions
        setResultConverter(this::handleButton);
    }

    private void buildContent() {
        // === Guideline defaults section ===
        Label guidelineHeader = sectionLabel("Default Guidelines");
        Label guidelineHint = hintLabel("Checked guidelines will be enabled by default on startup.");

        ListView<GuidelineToggle> guidelineList = new ListView<>();
        guidelineList.setPrefHeight(220);
        guidelineList.setCellFactory(CheckBoxListCell.forListView(item -> item.selected));

        for (DesignGuideline g : guidelines) {
            SimpleBooleanProperty prop = new SimpleBooleanProperty(config.isGuidelineEnabled(g.name()));
            guidelineToggles.put(g.name(), prop);
            guidelineList.getItems().add(new GuidelineToggle(g.name(), g.description(), prop));
        }

        // Tooltip on each guideline
        guidelineList.setCellFactory(lv -> {
            CheckBoxListCell<GuidelineToggle> cell = new CheckBoxListCell<>(item -> item.selected);
            cell.itemProperty().addListener((obs, old, item) -> {
                if (item != null) {
                    cell.setTooltip(new Tooltip(item.description));
                }
            });
            return cell;
        });

        // Select All / Deselect All buttons for guidelines
        Button selectAll = new Button("Select All");
        selectAll.setOnAction(e -> guidelineToggles.values().forEach(p -> p.set(true)));
        Button deselectAll = new Button("Deselect All");
        deselectAll.setOnAction(e -> guidelineToggles.values().forEach(p -> p.set(false)));
        HBox guidelineBtnBar = new HBox(8, selectAll, deselectAll);

        VBox guidelineSection = new VBox(6, guidelineHeader, guidelineHint, guidelineList, guidelineBtnBar);

        // === Filter defaults section ===
        Label filterHeader = sectionLabel("Default Severity Filters");
        Label filterHint = hintLabel("Checked filters will be active when results are first displayed.");

        showErrors    = new CheckBox("Show Errors");
        showWarnings  = new CheckBox("Show Warnings");
        showAdvisories = new CheckBox("Show Advisories");
        showPasses    = new CheckBox("Show Passes");

        VBox filterSection = new VBox(6, filterHeader, filterHint,
                showErrors, showWarnings, showAdvisories, showPasses);

        // === View preferences section ===
        Label viewHeader = sectionLabel("Default View");

        ToggleGroup viewGroup = new ToggleGroup();
        viewTree = new RadioButton("Tree View");
        viewTree.setToggleGroup(viewGroup);
        viewTable = new RadioButton("Table View");
        viewTable.setToggleGroup(viewGroup);

        sortBySev = new CheckBox("Sort by severity (errors first)");

        VBox viewSection = new VBox(6, viewHeader, viewTree, viewTable, sortBySev);

        // === Config file info ===
        Label infoHeader = sectionLabel("Configuration Files");
        Label globalPath = hintLabel("Global: " + configService.getGlobalConfigPath());
        Label projectPath;
        if (configService.hasProjectDir()) {
            projectPath = hintLabel("Project: " + configService.getProjectConfigDir().resolve("designlint.json"));
        } else {
            projectPath = hintLabel("Project: (load classes first to enable per-project config)");
        }

        VBox infoSection = new VBox(4, infoHeader, globalPath, projectPath);

        // === Layout: guidelines on left, filters+view on right ===
        VBox rightColumn = new VBox(16, filterSection, viewSection, infoSection);
        rightColumn.setPadding(new Insets(0, 0, 0, 12));

        HBox mainLayout = new HBox(16, guidelineSection, rightColumn);
        mainLayout.setPadding(new Insets(12));

        getDialogPane().setContent(mainLayout);
        getDialogPane().setPrefWidth(700);
        getDialogPane().setPrefHeight(500);
    }

    private void buildButtons() {
        // Standard Save and Cancel buttons
        getDialogPane().getButtonTypes().addAll(
                ButtonType.OK,     // "Save as Defaults" (global)
                ButtonType.CANCEL
        );

        // Rename OK to "Save as Defaults"
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Save as Defaults");

        // Add "Save for This Project" button (only enabled if classes are loaded)
        if (configService.hasProjectDir()) {
            getDialogPane().getButtonTypes().add(SAVE_PROJECT);
        }
    }

    /** Apply current config values to the dialog controls. */
    private void applyCurrentConfig() {
        // Guidelines are already applied during list construction

        // Filters
        showErrors.setSelected(config.getFilters().isShowErrors());
        showWarnings.setSelected(config.getFilters().isShowWarnings());
        showAdvisories.setSelected(config.getFilters().isShowAdvisories());
        showPasses.setSelected(config.getFilters().isShowPasses());

        // View
        if ("table".equals(config.getViewMode())) {
            viewTable.setSelected(true);
        } else {
            viewTree.setSelected(true);
        }
        sortBySev.setSelected(config.isSortBySeverity());
    }

    /** Collect dialog values back into the config object. */
    private void applyDialogToConfig() {
        // Guidelines
        Map<String, Boolean> guidelineMap = new LinkedHashMap<>();
        for (var entry : guidelineToggles.entrySet()) {
            guidelineMap.put(entry.getKey(), entry.getValue().get());
        }
        config.setGuidelines(guidelineMap);

        // Filters
        config.getFilters().setShowErrors(showErrors.isSelected());
        config.getFilters().setShowWarnings(showWarnings.isSelected());
        config.getFilters().setShowAdvisories(showAdvisories.isSelected());
        config.getFilters().setShowPasses(showPasses.isSelected());

        // View
        config.setViewMode(viewTable.isSelected() ? "table" : "tree");
        config.setSortBySeverity(sortBySev.isSelected());
    }

    /** Handle button press: save to the appropriate location. */
    private ButtonType handleButton(ButtonType buttonType) {
        if (buttonType == ButtonType.OK) {
            applyDialogToConfig();
            configService.saveGlobal();
        } else if (buttonType == SAVE_PROJECT) {
            applyDialogToConfig();
            configService.saveProject();
            configService.saveGlobal();  // also save globally
        }
        return buttonType;
    }

    /**
     * Whether settings were saved (OK or Save Project was clicked).
     * The caller uses this to know if the UI should refresh.
     */
    public static boolean wasSaved(ButtonType result) {
        return result == ButtonType.OK || result == SAVE_PROJECT;
    }

    // --- Helpers ---

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 13));
        return label;
    }

    private Label hintLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
        label.setWrapText(true);
        return label;
    }

    /**
     * Wrapper for guidelines in the settings dialog checkbox list.
     */
    private static class GuidelineToggle {
        final String name;
        final String description;
        final SimpleBooleanProperty selected;

        GuidelineToggle(String name, String description, SimpleBooleanProperty selected) {
            this.name = name;
            this.description = description;
            this.selected = selected;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
