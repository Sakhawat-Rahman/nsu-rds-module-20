package edu.nsu.rds.administration.controller;

import edu.nsu.rds.administration.config.DatabaseConnection;
import edu.nsu.rds.administration.dao.CalendarDAO;
import edu.nsu.rds.administration.dao.MonitoringDAO;
import edu.nsu.rds.administration.dao.ParameterDAO;
import edu.nsu.rds.administration.model.AcademicCalendarEvent;
import edu.nsu.rds.administration.model.MonitoringSample;
import edu.nsu.rds.administration.model.SystemParameter;
import edu.nsu.rds.administration.service.CalendarService;
import edu.nsu.rds.administration.service.MonitoringService;
import edu.nsu.rds.administration.service.ParameterService;
import edu.nsu.rds.administration.util.DateTimeUtils;
import edu.nsu.rds.administration.util.ValidationException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdminDashboardView {
    private static final String CURRENT_OPERATOR = "admin · 2614160642";
    private static final String OPERATOR_ID = "admin";

    // 3 Services & DAOs strictly aligned to Sir's 3 tables
    private final ParameterService parameterService = new ParameterService(new ParameterDAO());
    private final CalendarService calendarService = new CalendarService(new CalendarDAO());
    private final MonitoringService monitoringService = new MonitoringService(new MonitoringDAO());

    // Observable Lists for tables
    private final ObservableList<SystemParameter> parameterList = FXCollections.observableArrayList();
    private final ObservableList<AcademicCalendarEvent> calendarList = FXCollections.observableArrayList();
    private final ObservableList<MonitoringSample> sampleList = FXCollections.observableArrayList();

    // Active Selection State (prevents auto-sync from wiping edits)
    private String currentSelectedParamKey = "registration.open";
    private Long currentSelectedEventId = null;
    private boolean isAutoSyncing = false;

    // Top Utility Bar Navigation
    private final List<Button> navButtons = new ArrayList<>();
    private StackPane workspaceHost;
    private Node parametersView;
    private Node calendarView;
    private Node monitoringView;

    // Top Bar Status Nodes
    private Region connectionDot;
    private Label dbNameLabel;

    // Tables
    private TableView<SystemParameter> parameterTable;
    private TableView<AcademicCalendarEvent> calendarTable;
    private TableView<MonitoringSample> sampleTable;
    private Timeline autoSyncTimeline;

    // 1. Parameters View Nodes
    private Label paramRegValueLabel;
    private Label paramSemValueLabel;
    private Label paramMaintValueLabel;
    private TextField paramInspectorKeyField;
    private Label paramInspectorDescLabel;
    private TextField paramInspectorValField;
    private HBox paramQuickBoolRow;

    // 2. Calendar View Nodes
    private Label calTotalLabel;
    private Label calNextDeadlineLabel;
    private ComboBox<String> calFilterSemBox;
    private TextField calSearchField;
    private ComboBox<String> calFormSemBox;
    private TextField calFormNameField;
    private DatePicker calFormDataPicker;
    private ComboBox<String> calFormTypeBox;
    private TextField calFormDescField;

    // 3. Monitoring View Nodes
    private Label monUsersLabel;
    private Label monLatencyLabel;
    private Label monErrorsLabel;
    private TextField monUsersField;
    private TextField monLatencyField;
    private TextField monErrorsField;

    public Parent createContent() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #FAFAFA;");

        // 1. Razor-Thin 44px Top Utility Bar
        HBox topBar = buildTopUtilityBar();
        root.setTop(topBar);

        // 2. Center Dynamic Workspace
        workspaceHost = new StackPane();
        workspaceHost.setStyle("-fx-background-color: #FAFAFA;");

        parametersView = buildParametersView();
        calendarView = buildCalendarView();
        monitoringView = buildMonitoringView();

        workspaceHost.getChildren().addAll(parametersView, calendarView, monitoringView);
        switchView(parametersView, navButtons.get(0));

        root.setCenter(workspaceHost);

        // Initial Data Load
        try {
            refreshAllData();
        } catch (Throwable t) {
            System.err.println("Notice during initial data refresh: " + t.getMessage());
        }

        // 3-Second Background Auto-Sync
        startAutoSync();

        return root;
    }

    private void startAutoSync() {
        if (autoSyncTimeline != null) {
            autoSyncTimeline.stop();
        }
        autoSyncTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            try {
                isAutoSyncing = true;
                refreshAllData();
            } catch (Throwable ignored) {
            } finally {
                isAutoSyncing = false;
            }
        }));
        autoSyncTimeline.setCycleCount(Timeline.INDEFINITE);
        autoSyncTimeline.play();
    }

    // =========================================================================
    // RAZOR-THIN 44px TOP UTILITY BAR (#FFFFFF, bottom border 1px solid #E4E4E7)
    // =========================================================================
    private HBox buildTopUtilityBar() {
        HBox bar = new HBox(12);
        bar.getStyleClass().add("top-bar");

        // Brand Title
        Label brandTitle = new Label("NSU RDS // MODULE 20");
        brandTitle.getStyleClass().add("top-bar-title");

        // Divider
        Region div1 = new Region();
        div1.getStyleClass().add("top-bar-divider");

        // Segmented Squareish Navigation Tabs (Strictly Sir's 3 modules)
        Button btnParams = createNavSegmentButton("Parameters", () -> switchView(parametersView, navButtons.get(0)));
        Button btnCalendar = createNavSegmentButton("Calendar", () -> switchView(calendarView, navButtons.get(1)));
        Button btnMonitoring = createNavSegmentButton("Monitoring", () -> switchView(monitoringView, navButtons.get(2)));

        navButtons.clear();
        navButtons.add(btnParams);
        navButtons.add(btnCalendar);
        navButtons.add(btnMonitoring);

        HBox navBox = new HBox(4, btnParams, btnCalendar, btnMonitoring);
        navBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Right Utility: Connection Indicator
        connectionDot = new Region();
        connectionDot.getStyleClass().add("connection-dot");

        dbNameLabel = new Label("PG: nsu_rds");
        dbNameLabel.setStyle("-fx-font-family: 'Consolas', 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-text-fill: #71717A;");

        HBox dbIndicator = new HBox(6, connectionDot, dbNameLabel);
        dbIndicator.setAlignment(Pos.CENTER_LEFT);

        Region div2 = new Region();
        div2.getStyleClass().add("top-bar-divider");

        // Manual Refresh Button
        Button refreshBtn = new Button("↻ Refresh");
        refreshBtn.getStyleClass().add("btn-icon-compact");
        refreshBtn.setOnAction(e -> refreshAllData());

        Region div3 = new Region();
        div3.getStyleClass().add("top-bar-divider");

        // Minimal Monochrome Operator Tag
        Label operatorTag = new Label(CURRENT_OPERATOR);
        operatorTag.getStyleClass().add("operator-badge");

        bar.getChildren().addAll(brandTitle, div1, navBox, spacer, dbIndicator, div2, refreshBtn, div3, operatorTag);
        return bar;
    }

    private Button createNavSegmentButton(String label, Runnable action) {
        Button btn = new Button(label);
        btn.getStyleClass().add("nav-segment-btn");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void switchView(Node targetView, Button activeBtn) {
        for (Node child : workspaceHost.getChildren()) {
            child.setVisible(child == targetView);
            child.setManaged(child == targetView);
        }
        for (Button b : navButtons) {
            b.getStyleClass().remove("nav-segment-btn-active");
            if (!b.getStyleClass().contains("nav-segment-btn")) {
                b.getStyleClass().add("nav-segment-btn");
            }
        }
        activeBtn.getStyleClass().remove("nav-segment-btn");
        if (!activeBtn.getStyleClass().contains("nav-segment-btn-active")) {
            activeBtn.getStyleClass().add("nav-segment-btn-active");
        }
    }

    // =========================================================================
    // 1. PARAMETERS VIEW (system_parameters)
    // =========================================================================
    private Node buildParametersView() {
        VBox layout = new VBox(12);
        layout.getStyleClass().add("workspace-container");

        // Top Metric Strip: 3 compact squareish stat boxes
        HBox statStrip = new HBox(12);
        statStrip.setAlignment(Pos.CENTER_LEFT);

        paramRegValueLabel = new Label("OPEN");
        paramRegValueLabel.getStyleClass().add("stat-box-value");
        VBox stat1 = createStatBox("REGISTRATION", paramRegValueLabel, "registration.open");

        paramSemValueLabel = new Label("FALL2026");
        paramSemValueLabel.getStyleClass().add("stat-box-value");
        VBox stat2 = createStatBox("CURRENT SEMESTER", paramSemValueLabel, "semester.current");

        paramMaintValueLabel = new Label("OFF");
        paramMaintValueLabel.getStyleClass().add("stat-box-value");
        VBox stat3 = createStatBox("MAINTENANCE", paramMaintValueLabel, "maintenance_mode");

        HBox.setHgrow(stat1, Priority.ALWAYS);
        HBox.setHgrow(stat2, Priority.ALWAYS);
        HBox.setHgrow(stat3, Priority.ALWAYS);
        statStrip.getChildren().addAll(stat1, stat2, stat3);

        // Split Workspace: Flat Table (Left) + 320px Inspector (Right)
        HBox splitWorkspace = new HBox(12);
        VBox.setVgrow(splitWorkspace, Priority.ALWAYS);

        // Flat TableView Container
        VBox tableContainer = new VBox();
        tableContainer.getStyleClass().add("table-panel");
        HBox.setHgrow(tableContainer, Priority.ALWAYS);

        parameterTable = new TableView<>(parameterList);
        parameterTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(parameterTable, Priority.ALWAYS);

        TableColumn<SystemParameter, String> colKey = new TableColumn<>("PARAM_KEY");
        colKey.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getParamKey()));
        colKey.setCellFactory(col -> createMonoCell());
        colKey.setMinWidth(170);

        TableColumn<SystemParameter, String> colVal = new TableColumn<>("PARAM_VALUE");
        colVal.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getParamValue()));
        colVal.setCellFactory(col -> createBoldMonoCell());
        colVal.setMinWidth(140);

        TableColumn<SystemParameter, String> colDesc = new TableColumn<>("DESCRIPTION");
        colDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
        colDesc.setMinWidth(260);

        TableColumn<SystemParameter, String> colBy = new TableColumn<>("UPDATED_BY");
        colBy.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUpdatedBy()));
        colBy.setCellFactory(col -> createMonoCell());
        colBy.setMinWidth(90);

        TableColumn<SystemParameter, String> colAt = new TableColumn<>("UPDATED_AT");
        colAt.setCellValueFactory(d -> new SimpleStringProperty(DateTimeUtils.format(d.getValue().getUpdatedAt())));
        colAt.setCellFactory(col -> createMonoCell());
        colAt.setMinWidth(150);

        parameterTable.getColumns().addAll(colKey, colVal, colDesc, colBy, colAt);

        // Row double click: quick toggle for boolean or focus value field
        parameterTable.setRowFactory(tv -> {
            TableRow<SystemParameter> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    SystemParameter item = row.getItem();
                    if (item != null) {
                        String k = item.getParamKey();
                        if (k.equalsIgnoreCase("registration.open") || k.equalsIgnoreCase("maintenance_mode")) {
                            String toggled = "true".equalsIgnoreCase(item.getParamValue()) ? "false" : "true";
                            paramInspectorValField.setText(toggled);
                            saveParameterAction();
                        } else {
                            paramInspectorValField.requestFocus();
                            paramInspectorValField.selectAll();
                        }
                    }
                }
            });
            return row;
        });

        tableContainer.getChildren().add(parameterTable);

        // Right 320px Inspector Panel
        VBox inspector = new VBox(14);
        inspector.getStyleClass().add("inspector-panel");

        Label inspectorHeader = new Label("PARAMETER INSPECTOR");
        inspectorHeader.getStyleClass().add("inspector-header");
        inspectorHeader.setMaxWidth(Double.MAX_VALUE);

        // Form Fields
        VBox keyGroup = new VBox(4);
        Label keyLabel = new Label("PARAMETER KEY");
        keyLabel.getStyleClass().add("inspector-field-label");
        paramInspectorKeyField = new TextField("registration.open");
        paramInspectorKeyField.setEditable(false);
        paramInspectorKeyField.setStyle("-fx-background-color: #F4F4F5; -fx-text-fill: #71717A; -fx-font-family: 'Consolas', 'JetBrains Mono', monospace;");
        keyGroup.getChildren().addAll(keyLabel, paramInspectorKeyField);

        VBox descGroup = new VBox(4);
        Label descLabel = new Label("DESCRIPTION");
        descLabel.getStyleClass().add("inspector-field-label");
        paramInspectorDescLabel = new Label("Whether course registration is open for the current semester");
        paramInspectorDescLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #71717A;");
        paramInspectorDescLabel.setWrapText(true);
        descGroup.getChildren().addAll(descLabel, paramInspectorDescLabel);

        VBox valGroup = new VBox(6);
        Label valLabel = new Label("PARAMETER VALUE");
        valLabel.getStyleClass().add("inspector-field-label");
        paramInspectorValField = new TextField("true");
        paramInspectorValField.getStyleClass().add("font-mono");
        paramInspectorValField.setPromptText("Enter new value...");
        paramInspectorValField.setOnAction(e -> saveParameterAction());

        // Quick Boolean Action Buttons (Set true, Set false, Toggle & Save)
        paramQuickBoolRow = new HBox(6);
        paramQuickBoolRow.setAlignment(Pos.CENTER_LEFT);

        Button quickTrueBtn = new Button("Set \"true\"");
        quickTrueBtn.getStyleClass().add("btn-secondary");
        quickTrueBtn.setOnAction(e -> {
            paramInspectorValField.setText("true");
            saveParameterAction();
        });

        Button quickFalseBtn = new Button("Set \"false\"");
        quickFalseBtn.getStyleClass().add("btn-secondary");
        quickFalseBtn.setOnAction(e -> {
            paramInspectorValField.setText("false");
            saveParameterAction();
        });

        Button quickToggleBtn = new Button("⚡ Toggle & Save");
        quickToggleBtn.getStyleClass().add("btn-secondary");
        quickToggleBtn.setOnAction(e -> {
            String curr = paramInspectorValField.getText().trim();
            String flipped = "true".equalsIgnoreCase(curr) ? "false" : "true";
            paramInspectorValField.setText(flipped);
            saveParameterAction();
        });

        paramQuickBoolRow.getChildren().addAll(quickTrueBtn, quickFalseBtn, quickToggleBtn);
        valGroup.getChildren().addAll(valLabel, paramInspectorValField, paramQuickBoolRow);

        VBox byGroup = new VBox(4);
        Label byLabel = new Label("UPDATED BY");
        byLabel.getStyleClass().add("inspector-field-label");
        TextField byField = new TextField(OPERATOR_ID);
        byField.setEditable(false);
        byField.setStyle("-fx-background-color: #F4F4F5; -fx-text-fill: #71717A; -fx-font-family: 'Consolas', 'JetBrains Mono', monospace;");
        byGroup.getChildren().addAll(byLabel, byField);

        Button saveParamBtn = new Button("Save Parameter");
        saveParamBtn.getStyleClass().add("btn-primary");
        saveParamBtn.setMaxWidth(Double.MAX_VALUE);
        saveParamBtn.setOnAction(e -> saveParameterAction());

        // Table Selection Listener (Guarded against auto-sync clobbering)
        parameterTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (isAutoSyncing) {
                return; // NEVER OVERWRITE USER INPUT DURING AUTO-SYNC
            }
            if (newVal != null) {
                loadParameterIntoInspector(newVal);
            }
        });

        Region formSpacer = new Region();
        VBox.setVgrow(formSpacer, Priority.ALWAYS);

        inspector.getChildren().addAll(inspectorHeader, keyGroup, descGroup, valGroup, byGroup, formSpacer, saveParamBtn);

        splitWorkspace.getChildren().addAll(tableContainer, inspector);
        layout.getChildren().addAll(statStrip, splitWorkspace);
        return layout;
    }

    private void loadParameterIntoInspector(SystemParameter param) {
        currentSelectedParamKey = param.getParamKey();
        paramInspectorKeyField.setText(param.getParamKey());
        paramInspectorDescLabel.setText(param.getDescription());
        paramInspectorValField.setText(param.getParamValue());

        boolean isBool = param.getParamKey().equalsIgnoreCase("registration.open") ||
                         param.getParamKey().equalsIgnoreCase("maintenance_mode");
        if (paramQuickBoolRow != null) {
            paramQuickBoolRow.setVisible(isBool);
            paramQuickBoolRow.setManaged(isBool);
        }
    }

    private void saveParameterAction() {
        String key = currentSelectedParamKey;
        if (key == null || key.isBlank() || key.startsWith("Select")) {
            SystemParameter selected = parameterTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                key = selected.getParamKey();
            } else if (paramInspectorKeyField != null && !paramInspectorKeyField.getText().isBlank() && !paramInspectorKeyField.getText().startsWith("Select")) {
                key = paramInspectorKeyField.getText().trim();
            }
        }

        if (key == null || key.isBlank() || key.startsWith("Select")) {
            if (!parameterList.isEmpty()) {
                key = parameterList.get(0).getParamKey();
                currentSelectedParamKey = key;
                parameterTable.getSelectionModel().select(0);
            } else {
                showAlert(Alert.AlertType.WARNING, "No Parameter Selected", "Please click a parameter row in the table first.");
                return;
            }
        }

        String newVal = paramInspectorValField.getText();
        if (newVal == null || newVal.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Value", "Parameter value must not be empty.");
            return;
        }

        try {
            boolean updated = parameterService.updateParameter(key, newVal.trim(), OPERATOR_ID);
            if (updated) {
                currentSelectedParamKey = key;
                refreshAllData();
                showAlert(Alert.AlertType.INFORMATION, "Parameter Saved",
                        "Parameter '" + key + "' successfully updated to: " + newVal.trim());
            } else {
                showAlert(Alert.AlertType.ERROR, "Update Failed", "Database reported 0 rows updated for key '" + key + "'.");
            }
        } catch (ValidationException ex) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", ex.getMessage());
        }
    }

    // =========================================================================
    // 2. CALENDAR VIEW (academic_calendar)
    // =========================================================================
    private Node buildCalendarView() {
        VBox layout = new VBox(12);
        layout.getStyleClass().add("workspace-container");

        // Top Metric Strip: 3 compact stat boxes
        HBox statStrip = new HBox(12);
        statStrip.setAlignment(Pos.CENTER_LEFT);

        calTotalLabel = new Label("5 Scheduled");
        calTotalLabel.getStyleClass().add("stat-box-value");
        VBox stat1 = createStatBox("TOTAL EVENTS", calTotalLabel, "Active term schedule");

        calNextDeadlineLabel = new Label("Add/drop deadline");
        calNextDeadlineLabel.getStyleClass().add("stat-box-value");
        calNextDeadlineLabel.setStyle("-fx-font-size: 14px;");
        VBox stat2 = createStatBox("NEXT DEADLINE", calNextDeadlineLabel, "Strict module 05 rule (2026-11-30)");

        // Stat Box 3: Filter & Search Controls
        VBox stat3 = new VBox(4);
        stat3.getStyleClass().add("stat-box");
        Label filterTitle = new Label("SEMESTER FILTER & SEARCH");
        filterTitle.getStyleClass().add("stat-box-title");

        HBox filterRow = new HBox(6);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        calFilterSemBox = new ComboBox<>(FXCollections.observableArrayList("ALL", "FALL2026", "SPRING2027", "SUMMER2027"));
        calFilterSemBox.setValue("ALL");
        calFilterSemBox.setPrefWidth(110);
        calFilterSemBox.setOnAction(e -> applyCalendarFilter());

        calSearchField = new TextField();
        calSearchField.setPromptText("Search event...");
        calSearchField.textProperty().addListener((obs, o, n) -> applyCalendarFilter());
        HBox.setHgrow(calSearchField, Priority.ALWAYS);

        filterRow.getChildren().addAll(calFilterSemBox, calSearchField);
        stat3.getChildren().addAll(filterTitle, filterRow);

        HBox.setHgrow(stat1, Priority.ALWAYS);
        HBox.setHgrow(stat2, Priority.ALWAYS);
        HBox.setHgrow(stat3, Priority.ALWAYS);
        statStrip.getChildren().addAll(stat1, stat2, stat3);

        // Split Workspace
        HBox splitWorkspace = new HBox(12);
        VBox.setVgrow(splitWorkspace, Priority.ALWAYS);

        // Flat TableView Container
        VBox tableContainer = new VBox();
        tableContainer.getStyleClass().add("table-panel");
        HBox.setHgrow(tableContainer, Priority.ALWAYS);

        calendarTable = new TableView<>(calendarList);
        calendarTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(calendarTable, Priority.ALWAYS);

        TableColumn<AcademicCalendarEvent, String> colSem = new TableColumn<>("SEMESTER");
        colSem.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSemesterCode()));
        colSem.setCellFactory(col -> createMonoCell());
        colSem.setMaxWidth(100);

        TableColumn<AcademicCalendarEvent, String> colName = new TableColumn<>("EVENT NAME");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEventName()));
        colName.setMinWidth(180);

        TableColumn<AcademicCalendarEvent, String> colDate = new TableColumn<>("EVENT DATE");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(DateTimeUtils.format(d.getValue().getEventDate())));
        colDate.setCellFactory(col -> createMonoCell());
        colDate.setMaxWidth(110);

        TableColumn<AcademicCalendarEvent, String> colType = new TableColumn<>("EVENT TYPE");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEventType()));
        colType.setCellFactory(col -> createEventTypeTagCell());
        colType.setMaxWidth(100);

        TableColumn<AcademicCalendarEvent, String> colDesc = new TableColumn<>("DESCRIPTION");
        colDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
        colDesc.setMinWidth(240);

        calendarTable.getColumns().addAll(colSem, colName, colDate, colType, colDesc);
        tableContainer.getChildren().add(calendarTable);

        // Right 320px Inspector Panel
        VBox inspector = new VBox(12);
        inspector.getStyleClass().add("inspector-panel");

        Label inspectorHeader = new Label("EVENT INSPECTOR");
        inspectorHeader.getStyleClass().add("inspector-header");
        inspectorHeader.setMaxWidth(Double.MAX_VALUE);

        // Form fields
        VBox semGroup = new VBox(4);
        Label semLabel = new Label("SEMESTER CODE");
        semLabel.getStyleClass().add("inspector-field-label");
        calFormSemBox = new ComboBox<>(FXCollections.observableArrayList("FALL2026", "SPRING2027", "SUMMER2027"));
        calFormSemBox.setValue("FALL2026");
        calFormSemBox.setMaxWidth(Double.MAX_VALUE);
        semGroup.getChildren().addAll(semLabel, calFormSemBox);

        VBox nameGroup = new VBox(4);
        Label nameLabel = new Label("EVENT NAME");
        nameLabel.getStyleClass().add("inspector-field-label");
        calFormNameField = new TextField();
        calFormNameField.setPromptText("e.g. Add/drop deadline");
        nameGroup.getChildren().addAll(nameLabel, calFormNameField);

        VBox dateGroup = new VBox(4);
        Label dateLabel = new Label("EVENT DATE");
        dateLabel.getStyleClass().add("inspector-field-label");
        calFormDataPicker = new DatePicker(LocalDate.of(2026, 11, 30));
        calFormDataPicker.setMaxWidth(Double.MAX_VALUE);
        dateGroup.getChildren().addAll(dateLabel, calFormDataPicker);

        VBox typeGroup = new VBox(4);
        Label typeLabel = new Label("EVENT TYPE");
        typeLabel.getStyleClass().add("inspector-field-label");
        calFormTypeBox = new ComboBox<>(FXCollections.observableArrayList("START", "DEADLINE", "HOLIDAY", "EXAM", "END"));
        calFormTypeBox.setValue("DEADLINE");
        calFormTypeBox.setMaxWidth(Double.MAX_VALUE);
        typeGroup.getChildren().addAll(typeLabel, calFormTypeBox);

        VBox descGroup = new VBox(4);
        Label descLabel = new Label("DESCRIPTION");
        descLabel.getStyleClass().add("inspector-field-label");
        calFormDescField = new TextField();
        calFormDescField.setPromptText("Optional description...");
        descGroup.getChildren().addAll(descLabel, calFormDescField);

        // Selection Listener (Guarded against auto-sync clobbering)
        calendarTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (isAutoSyncing) {
                return; // NEVER OVERWRITE CALENDAR FORM DURING AUTO-SYNC
            }
            if (newVal != null) {
                currentSelectedEventId = newVal.getEventId();
                calFormSemBox.setValue(newVal.getSemesterCode());
                calFormNameField.setText(newVal.getEventName());
                if (newVal.getEventDate() != null) calFormDataPicker.setValue(newVal.getEventDate());
                calFormTypeBox.setValue(newVal.getEventType());
                calFormDescField.setText(newVal.getDescription() != null ? newVal.getDescription() : "");
            }
        });

        // Action Buttons Grid
        GridPane btnGrid = new GridPane();
        btnGrid.setHgap(8);
        btnGrid.setVgap(8);

        ColumnConstraints bCol1 = new ColumnConstraints();
        bCol1.setPercentWidth(50);
        ColumnConstraints bCol2 = new ColumnConstraints();
        bCol2.setPercentWidth(50);
        btnGrid.getColumnConstraints().addAll(bCol1, bCol2);

        Button addBtn = new Button("+ Add Event");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> {
            try {
                AcademicCalendarEvent created = calendarService.createEvent(
                        calFormSemBox.getValue(),
                        calFormNameField.getText(),
                        calFormDataPicker.getValue(),
                        calFormTypeBox.getValue(),
                        calFormDescField.getText()
                );
                currentSelectedEventId = created.getEventId();
                refreshAllData();
                showAlert(Alert.AlertType.INFORMATION, "Created", "Academic milestone created.");
            } catch (ValidationException ex) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", ex.getMessage());
            }
        });

        Button updateBtn = new Button("Update");
        updateBtn.getStyleClass().add("btn-secondary");
        updateBtn.setMaxWidth(Double.MAX_VALUE);
        updateBtn.setOnAction(e -> {
            Long eventId = currentSelectedEventId;
            AcademicCalendarEvent selected = calendarTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                eventId = selected.getEventId();
            }
            if (eventId == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a calendar event to update.");
                return;
            }
            try {
                calendarService.updateEvent(
                        eventId,
                        calFormSemBox.getValue(),
                        calFormNameField.getText(),
                        calFormDataPicker.getValue(),
                        calFormTypeBox.getValue(),
                        calFormDescField.getText()
                );
                refreshAllData();
                showAlert(Alert.AlertType.INFORMATION, "Updated", "Calendar event updated.");
            } catch (ValidationException ex) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", ex.getMessage());
            }
        });

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-destructive");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setOnAction(e -> {
            Long eventId = currentSelectedEventId;
            AcademicCalendarEvent selected = calendarTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                eventId = selected.getEventId();
            }
            if (eventId == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an event to delete.");
                return;
            }
            try {
                calendarService.deleteEvent(eventId);
                currentSelectedEventId = null;
                refreshAllData();
                showAlert(Alert.AlertType.INFORMATION, "Deleted", "Event deleted successfully.");
            } catch (ValidationException ex) {
                showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
            }
        });

        Button clearBtn = new Button("Clear Form");
        clearBtn.getStyleClass().add("btn-secondary");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setOnAction(e -> {
            calendarTable.getSelectionModel().clearSelection();
            currentSelectedEventId = null;
            calFormNameField.clear();
            calFormDescField.clear();
            calFormDataPicker.setValue(LocalDate.now());
        });

        btnGrid.add(addBtn, 0, 0);
        btnGrid.add(updateBtn, 1, 0);
        btnGrid.add(deleteBtn, 0, 1);
        btnGrid.add(clearBtn, 1, 1);

        Region formSpacer = new Region();
        VBox.setVgrow(formSpacer, Priority.ALWAYS);

        inspector.getChildren().addAll(inspectorHeader, semGroup, nameGroup, dateGroup, typeGroup, descGroup, formSpacer, btnGrid);

        splitWorkspace.getChildren().addAll(tableContainer, inspector);
        layout.getChildren().addAll(statStrip, splitWorkspace);
        return layout;
    }

    private void applyCalendarFilter() {
        String sem = calFilterSemBox != null ? calFilterSemBox.getValue() : "ALL";
        String q = calSearchField != null ? calSearchField.getText().trim().toLowerCase() : "";

        List<AcademicCalendarEvent> filtered = calendarService.getEventsBySemester(sem).stream()
                .filter(ev -> q.isEmpty() ||
                              ev.getEventName().toLowerCase().contains(q) ||
                              (ev.getDescription() != null && ev.getDescription().toLowerCase().contains(q)) ||
                              ev.getEventType().toLowerCase().contains(q))
                .toList();
        calendarList.setAll(filtered);
    }

    // =========================================================================
    // 3. MONITORING VIEW (monitoring_samples)
    // =========================================================================
    private Node buildMonitoringView() {
        VBox layout = new VBox(12);
        layout.getStyleClass().add("workspace-container");

        // Top Metric Strip: 3 compact stat boxes
        HBox statStrip = new HBox(12);
        statStrip.setAlignment(Pos.CENTER_LEFT);

        monUsersLabel = new Label("51 Users");
        monUsersLabel.getStyleClass().add("stat-box-value");
        VBox stat1 = createStatBox("LATEST ACTIVE USERS", monUsersLabel, "Bounded threshold: 0..10,000");

        monLatencyLabel = new Label("15 ms");
        monLatencyLabel.getStyleClass().add("stat-box-value");
        VBox stat2 = createStatBox("LATEST DB LATENCY", monLatencyLabel, "Nominal connection pool health");

        monErrorsLabel = new Label("0 Errors");
        monErrorsLabel.getStyleClass().add("stat-box-value");
        VBox stat3 = createStatBox("TOTAL AUDIT ERRORS", monErrorsLabel, "Historical error count sum");

        HBox.setHgrow(stat1, Priority.ALWAYS);
        HBox.setHgrow(stat2, Priority.ALWAYS);
        HBox.setHgrow(stat3, Priority.ALWAYS);
        statStrip.getChildren().addAll(stat1, stat2, stat3);

        // Split Workspace
        HBox splitWorkspace = new HBox(12);
        VBox.setVgrow(splitWorkspace, Priority.ALWAYS);

        // Flat TableView Container
        VBox tableContainer = new VBox();
        tableContainer.getStyleClass().add("table-panel");
        HBox.setHgrow(tableContainer, Priority.ALWAYS);

        sampleTable = new TableView<>(sampleList);
        sampleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(sampleTable, Priority.ALWAYS);

        TableColumn<MonitoringSample, Number> colId = new TableColumn<>("SAMPLE_ID");
        colId.setCellValueFactory(d -> new SimpleLongProperty(d.getValue().getSampleId() != null ? d.getValue().getSampleId() : 0));
        colId.setCellFactory(col -> createMonoNumCell());
        colId.setMaxWidth(90);

        TableColumn<MonitoringSample, String> colTime = new TableColumn<>("SAMPLED_AT (TIMESTAMPTZ)");
        colTime.setCellValueFactory(d -> new SimpleStringProperty(DateTimeUtils.format(d.getValue().getSampledAt())));
        colTime.setCellFactory(col -> createMonoCell());
        colTime.setMinWidth(180);

        TableColumn<MonitoringSample, Number> colUsers = new TableColumn<>("ACTIVE_USERS");
        colUsers.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getActiveUsers()));
        colUsers.setCellFactory(col -> createMonoNumCell());
        colUsers.setMinWidth(120);

        TableColumn<MonitoringSample, Number> colLat = new TableColumn<>("DB_LATENCY_MS");
        colLat.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getDbLatencyMs()));
        colLat.setCellFactory(col -> createMonoNumCell());
        colLat.setMinWidth(130);

        TableColumn<MonitoringSample, Number> colErr = new TableColumn<>("ERROR_COUNT");
        colErr.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getErrorCount()));
        colErr.setCellFactory(col -> createMonoNumCell());
        colErr.setMinWidth(110);

        sampleTable.getColumns().addAll(colId, colTime, colUsers, colLat, colErr);
        tableContainer.getChildren().add(sampleTable);

        // Right 320px Inspector Panel
        VBox inspector = new VBox(12);
        inspector.getStyleClass().add("inspector-panel");

        Label inspectorHeader = new Label("TELEMETRY RECORDER");
        inspectorHeader.getStyleClass().add("inspector-header");
        inspectorHeader.setMaxWidth(Double.MAX_VALUE);

        VBox usersGroup = new VBox(4);
        Label usersLabel = new Label("ACTIVE USERS (0..10000)");
        usersLabel.getStyleClass().add("inspector-field-label");
        monUsersField = new TextField("50");
        monUsersField.getStyleClass().add("font-mono");
        usersGroup.getChildren().addAll(usersLabel, monUsersField);

        VBox latGroup = new VBox(4);
        Label latLabel = new Label("DB LATENCY MS (0..60000)");
        latLabel.getStyleClass().add("inspector-field-label");
        monLatencyField = new TextField("12");
        monLatencyField.getStyleClass().add("font-mono");
        latGroup.getChildren().addAll(latLabel, monLatencyField);

        VBox errGroup = new VBox(4);
        Label errLabel = new Label("ERROR COUNT (0..100000)");
        errLabel.getStyleClass().add("inspector-field-label");
        monErrorsField = new TextField("0");
        monErrorsField.getStyleClass().add("font-mono");
        errGroup.getChildren().addAll(errLabel, monErrorsField);

        Button recordBtn = new Button("Record Sample");
        recordBtn.getStyleClass().add("btn-primary");
        recordBtn.setMaxWidth(Double.MAX_VALUE);
        recordBtn.setOnAction(e -> {
            try {
                int users = Integer.parseInt(monUsersField.getText().trim());
                int lat = Integer.parseInt(monLatencyField.getText().trim());
                int err = Integer.parseInt(monErrorsField.getText().trim());

                monitoringService.recordSample(users, lat, err);
                refreshAllData();
                showAlert(Alert.AlertType.INFORMATION, "Recorded", "Telemetry sample persisted to monitoring_samples.");
            } catch (NumberFormatException nfe) {
                showAlert(Alert.AlertType.ERROR, "Number Format Error", "Please enter valid integers for users, latency, and errors.");
            } catch (ValidationException ve) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", ve.getMessage());
            }
        });

        // Quick Live Ping Button (Real JDBC Ping)
        Button livePingBtn = new Button("⚡ Quick Live Ping");
        livePingBtn.getStyleClass().add("btn-secondary");
        livePingBtn.setMaxWidth(Double.MAX_VALUE);
        livePingBtn.setOnAction(e -> performLivePing());

        Region formSpacer = new Region();
        VBox.setVgrow(formSpacer, Priority.ALWAYS);

        inspector.getChildren().addAll(inspectorHeader, usersGroup, latGroup, errGroup, formSpacer, recordBtn, livePingBtn);

        splitWorkspace.getChildren().addAll(tableContainer, inspector);
        layout.getChildren().addAll(statStrip, splitWorkspace);
        return layout;
    }

    private void performLivePing() {
        long start = System.currentTimeMillis();
        boolean reachable = DatabaseConnection.isDatabaseReachable();
        long elapsed = System.currentTimeMillis() - start;

        int latencyMs = reachable ? (int) Math.max(1, elapsed) : 999;
        int activeUsers = 40 + (int)(Math.random() * 25);
        int errors = reachable ? 0 : 1;

        try {
            monitoringService.recordSample(activeUsers, latencyMs, errors);
            refreshAllData();
            showAlert(Alert.AlertType.INFORMATION, "Live Ping Recorded",
                    "Measured live round-trip latency: " + latencyMs + " ms\nPersisted sample with " + activeUsers + " simulated active users.");
        } catch (ValidationException ex) {
            showAlert(Alert.AlertType.ERROR, "Ping Error", ex.getMessage());
        }
    }

    // =========================================================================
    // STAT BOX FACTORY HELPER
    // =========================================================================
    private VBox createStatBox(String titleText, Label valLabel, String contextText) {
        VBox box = new VBox(2);
        box.getStyleClass().add("stat-box");

        Label title = new Label(titleText);
        title.getStyleClass().add("stat-box-title");

        Label context = new Label(contextText);
        context.getStyleClass().add("stat-box-context");

        box.getChildren().addAll(title, valLabel, context);
        return box;
    }

    // =========================================================================
    // DATA REFRESH & SYNCHRONIZATION
    // =========================================================================
    private void refreshAllData() {
        // 1. Fetch Latest Data
        parameterList.setAll(parameterService.getAllParameters());
        calendarList.setAll(calendarService.getAllEvents());
        sampleList.setAll(monitoringService.getHistoricalSamples());

        // 2. Preserve / Set Selection in Parameters Table
        if (parameterTable != null && !parameterList.isEmpty()) {
            int targetIdx = -1;
            if (currentSelectedParamKey != null) {
                for (int i = 0; i < parameterList.size(); i++) {
                    if (parameterList.get(i).getParamKey().equalsIgnoreCase(currentSelectedParamKey)) {
                        targetIdx = i;
                        break;
                    }
                }
            }
            if (targetIdx == -1) {
                targetIdx = 0;
            }
            parameterTable.getSelectionModel().select(targetIdx);

            // If inspector not yet populated, populate with selected item
            SystemParameter currentParam = parameterList.get(targetIdx);
            if (paramInspectorKeyField != null && (paramInspectorKeyField.getText().startsWith("Select") || !paramInspectorKeyField.getText().equalsIgnoreCase(currentParam.getParamKey()))) {
                loadParameterIntoInspector(currentParam);
            }
        }

        // 3. Preserve / Set Selection in Calendar Table
        if (calendarTable != null && !calendarList.isEmpty()) {
            int targetCalIdx = -1;
            if (currentSelectedEventId != null) {
                for (int i = 0; i < calendarList.size(); i++) {
                    if (calendarList.get(i).getEventId().equals(currentSelectedEventId)) {
                        targetCalIdx = i;
                        break;
                    }
                }
            }
            if (targetCalIdx >= 0) {
                calendarTable.getSelectionModel().select(targetCalIdx);
            }
        }

        // 4. Database Connection Indicator
        boolean dbReachable = DatabaseConnection.isDatabaseReachable();
        if (dbReachable) {
            connectionDot.getStyleClass().remove("connection-dot-offline");
            if (!connectionDot.getStyleClass().contains("connection-dot")) {
                connectionDot.getStyleClass().add("connection-dot");
            }
            dbNameLabel.setText("PG: " + extractDbName(DatabaseConnection.getUrl()));
        } else {
            connectionDot.getStyleClass().remove("connection-dot");
            if (!connectionDot.getStyleClass().contains("connection-dot-offline")) {
                connectionDot.getStyleClass().add("connection-dot-offline");
            }
            dbNameLabel.setText("PG: OFFLINE (MEMORY)");
        }

        // 5. Parameters Top Strip
        boolean regOpen = parameterService.isRegistrationOpen();
        String currentSem = parameterService.getCurrentSemester();
        boolean maintenance = parameterService.isMaintenanceModeActive();

        if (paramRegValueLabel != null) {
            paramRegValueLabel.setText(regOpen ? "OPEN" : "CLOSED");
            paramRegValueLabel.setStyle(regOpen ? "-fx-text-fill: #09090B;" : "-fx-text-fill: #DC2626;");
        }
        if (paramSemValueLabel != null) {
            paramSemValueLabel.setText(currentSem);
        }
        if (paramMaintValueLabel != null) {
            paramMaintValueLabel.setText(maintenance ? "ACTIVE" : "OFF");
            paramMaintValueLabel.setStyle(maintenance ? "-fx-text-fill: #DC2626;" : "-fx-text-fill: #09090B;");
        }

        // 6. Calendar Top Strip
        if (calTotalLabel != null) {
            calTotalLabel.setText(calendarList.size() + " Scheduled");
        }
        if (calNextDeadlineLabel != null) {
            AcademicCalendarEvent deadline = calendarList.stream()
                    .filter(e -> "DEADLINE".equalsIgnoreCase(e.getEventType()) || "EXAM".equalsIgnoreCase(e.getEventType()))
                    .findFirst().orElse(!calendarList.isEmpty() ? calendarList.get(0) : null);
            if (deadline != null) {
                calNextDeadlineLabel.setText(deadline.getEventName() + " (" + DateTimeUtils.format(deadline.getEventDate()) + ")");
            } else {
                calNextDeadlineLabel.setText("No upcoming deadline");
            }
        }

        // 7. Monitoring Top Strip
        Optional<MonitoringSample> latestOpt = monitoringService.getLatestSample();
        if (latestOpt.isPresent()) {
            MonitoringSample latest = latestOpt.get();
            if (monUsersLabel != null) monUsersLabel.setText(latest.getActiveUsers() + " Users");
            if (monLatencyLabel != null) monLatencyLabel.setText(latest.getDbLatencyMs() + " ms");
        } else {
            if (monUsersLabel != null) monUsersLabel.setText("0 Users");
            if (monLatencyLabel != null) monLatencyLabel.setText("0 ms");
        }
        if (monErrorsLabel != null) {
            monErrorsLabel.setText(monitoringService.getTotalErrors() + " Errors");
        }
    }

    private String extractDbName(String url) {
        if (url == null) return "nsu_rds";
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash < 0) return "nsu_rds";
        int q = url.indexOf('?', lastSlash);
        return q >= 0 ? url.substring(lastSlash + 1, q) : url.substring(lastSlash + 1);
    }

    // =========================================================================
    // SQUAREISH TABLE CELL FACTORIES (Monospace & Tag Badges)
    // =========================================================================
    private static <T> TableCell<T, String> createMonoCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    getStyleClass().add("mono-cell");
                }
            }
        };
    }

    private static <T> TableCell<T, String> createBoldMonoCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    getStyleClass().add("mono-cell");
                    setStyle("-fx-font-weight: 800;");
                }
            }
        };
    }

    private static <T> TableCell<T, Number> createMonoNumCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(String.valueOf(item));
                    getStyleClass().add("mono-cell");
                }
            }
        };
    }

    private static TableCell<AcademicCalendarEvent, String> createEventTypeTagCell() {
        return new TableCell<>() {
            private final Label tag = new Label();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    tag.setText(item.toUpperCase());
                    tag.getStyleClass().clear();
                    tag.getStyleClass().addAll("tag-base", "tag-neutral");

                    switch (item.toUpperCase()) {
                        case "DEADLINE" -> {
                            tag.getStyleClass().remove("tag-neutral");
                            tag.getStyleClass().add("tag-alert");
                        }
                        case "START", "END" -> {
                            tag.getStyleClass().remove("tag-neutral");
                            tag.getStyleClass().add("tag-active");
                        }
                        case "EXAM" -> {
                            tag.getStyleClass().remove("tag-neutral");
                            tag.getStyleClass().add("tag-exam");
                        }
                    }
                    setGraphic(tag);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        };
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
