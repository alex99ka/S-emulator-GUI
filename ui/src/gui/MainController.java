package gui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;
import javafx.util.Pair;

import java.util.*;
import java.util.function.UnaryOperator;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.File;

import semulator.impl.api.skeleton.AbstractOpBasic;
import semulator.input.XmlTranslator.Factory;
import semulator.program.FunctionExecutor;
import semulator.execution.ProgramExecutorImpl;
import semulator.program.SprogramImpl;
import semulator.variable.VariableImpl;

public class MainController {
    // Engine classes available during whole app life
    private final Factory factory;
    private FunctionExecutor program;
    private FunctionExecutor programCopy;
    private FunctionExecutor programWork;
    List <FunctionExecutor> functions;
    private int maxDegree;
    private boolean darkTheme = false;
    private String highlightText;
    private VBox paramBox;
    private final List<TextField> paramFields = new ArrayList<>();
    private List<Pair<Integer, TreeMap<VariableImpl, Long>>> runListMap;
    private int currentStepIndex = 0;
    private int currentHighlightedStep = -1;
    private int runHistoryCounter = 0;
    private final ObservableList<ProgramHistoryRow> historyRunData = FXCollections.observableArrayList();
    private Spinner spinner;

    @FXML private VBox runBox;
    @FXML private HBox runButtonsBox;
    @FXML private Button runButton;
    @FXML private Button debugButton;
    @FXML private Button stepOverButton;
    @FXML private Button stepBackButton;
    @FXML private Button stopButton;
    @FXML private Button resumeButton;
    @FXML private TextField filePathField;
    @FXML private Button themeToggleButton;
    @FXML private Label statusBar;
    @FXML private ProgressBar progressBar;
    @FXML private Label expandLabel;
    @FXML private TextField expandField;
    @FXML private Button expandButton;
    @FXML private Button collapseButton;
    @FXML private ComboBox<String> highlightComboBox;
    @FXML private ComboBox<String> funcsComboBox;
    @FXML private TableView<InstructionRow> instructionTable;
    @FXML private TableColumn<InstructionRow, Integer> colNumber;
    @FXML private TableColumn<InstructionRow, String> colType;
    @FXML private TableColumn<InstructionRow, String> colLabel;
    @FXML private TableColumn<InstructionRow, String> colInstruction;
    @FXML private TableColumn<InstructionRow, Integer> colCycle;
    @FXML private TableColumn<InstructionRow, Boolean> colBreakpoint;
    @FXML private TableView<InstructionBaseRow> historyInstrTable;
    @FXML private TableColumn<InstructionBaseRow, Integer> colHistoryNumber;
    @FXML private TableColumn<InstructionBaseRow, String> colHistoryType;
    @FXML private TableColumn<InstructionBaseRow, String> colHistoryLabel;
    @FXML private TableColumn<InstructionBaseRow, String> colHistoryInstruction;
    @FXML private TableColumn<InstructionBaseRow, Integer> colHistoryCycle;
    @FXML private TableView<WatchDebugRow> debugTable;
    @FXML private TableColumn<WatchDebugRow, String> colVar;
    @FXML private TableColumn<WatchDebugRow, String> colValue;
    @FXML private TableView<ProgramHistoryRow> historyRunTable;
    @FXML private TableColumn<ProgramHistoryRow, Integer> colHistoryRunNumber;
    @FXML private TableColumn<ProgramHistoryRow, Integer> colHistoryRunDegree;
    @FXML private TableColumn<ProgramHistoryRow, String> colHistoryRunInput;
    @FXML private TableColumn<ProgramHistoryRow, Integer> colHistoryRunResult;
    @FXML private TableColumn<ProgramHistoryRow, Integer> colHistoryRunCycle;
    @FXML private StackPane stackPane;
    @FXML private RadioButton tableRadio;
    @FXML private RadioButton treeRadio;
    @FXML private ToggleGroup viewToggleGroup;
    @FXML private TreeView<OpTreeNode> programTree;

    public MainController() {

        factory = new Factory();
    }

    @FXML
    private void initialize() {
        setupToggleGroup();
        setupProgramTable();
        setupHistoryTable();
        setupWatchDebugTable();
        setupProgramHistory();
        historyRunTable.setItems(historyRunData);
        setupToolTips();
        setupTreeView();

        spinner = new Spinner();
        stackPane.getChildren().add(spinner);

        instructionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                AbstractOpBasic selectedOp = newSelection.getOp();
                showHistory(selectedOp);
            }
        });

        expandField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                expandField.setText(oldValue);
                return;
            }
            if (!newValue.isEmpty()) {
                try {
                    int value = Integer.parseInt(newValue);
                    if (value < 0 || value > maxDegree) {
                        expandField.setText(oldValue);
                    }
                } catch (NumberFormatException e) {
                    expandField.setText(oldValue);
                }
            }
        });

        highlightComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            onHighlightSelection(newValue);
        });

        funcsComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            onFuncsSelection(newValue);
        });
    }

    public FunctionExecutor getMainProgram() {
        return programCopy;
    }

    @FXML
    private void onRun() {
        if (checkAndConfirmParams()) {
            runRoutine();
            spinner.runWithOverlay(stackPane, () -> {
            });
        }
    }

    @FXML
    private void onDebug() {
        if (checkAndConfirmParams()) {
            expandButton.setDisable(true);
            collapseButton.setDisable(true);
            resetDebugVars();
            instructionTable.scrollTo(0);
            instructionTable.refresh();
            enableDebugControls(true);
            runDebugRoutine();
        }
    }

    @FXML
    private void onStepOver() {
        stepOverRoutine();
    }

    @FXML
    private void onStepBackButton() {
        if (currentStepIndex - 2 >= 0)
        {
            currentStepIndex -= 2;
            stepOverRoutine();
        }
    }

    @FXML
    private void onStopDebug() {
        stopDebugging(false);
        showStatus("Debug stopped.", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void onResumeDebug() {
        if (runListMap == null) return;

        stepOverButton.setDisable(false);

        while (currentStepIndex <= runListMap.size()) {
            stepOverRoutine();

            if (currentHighlightedStep == -1)
                break;
            // Optional: stop automatically if a breakpoint is reached
            InstructionRow currentRow = instructionTable.getItems().get(currentHighlightedStep);
            if (currentRow.getBreakpoint()) {
                statusBar.setText("Paused at breakpoint: " + currentRow.getLabel());
                break;
            }
        }
    }

    @FXML
    private void onLoadFile() {
        displayFile();
        String filePath = filePathField.getText();
        File file = validateFile(filePath);
        if (file == null) {
            filePathField.clear();
            return;
        }

        Task<FunctionExecutor> loadTask = createLoadTask(file);
        bindTaskToUI(loadTask);
        runTask(loadTask);
    }

    @FXML
    private void onExpand() {
        int setDegree;
        try {
            setDegree = Integer.parseInt(expandField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert("Invalid input", "Please enter a valid number", Alert.AlertType.ERROR);
            return;
        }

        if (setDegree >= program.getProgramDegree()) {
            showAlert("Invalid degree", "Max degree is " + program.getProgramDegree(), Alert.AlertType.ERROR);
            return;
        }

        expand(setDegree + 1);
    }

    @FXML
    private void onCollapse() {
        int setDegree=1;
        try {
            setDegree = Integer.parseInt(expandField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert("Invalid input", "Please enter a valid number", Alert.AlertType.ERROR);
            return;
        }

        if (setDegree == 0) {
            showAlert("Invalid degree", "Min degree is 0", Alert.AlertType.ERROR);
            return;
        }
        collapse(setDegree - 1);
    }

    @FXML
    private void onToggleTheme() {
        if (darkTheme) {
            // Switch to light theme
            themeToggleButton.getScene().getStylesheets().clear();
            themeToggleButton.getScene().getStylesheets().add(getClass().getResource("/css/style-light.css").toExternalForm());
            themeToggleButton.setText("Dark Theme");
            darkTheme = false;
        } else {
            // Switch to dark theme
            themeToggleButton.getScene().getStylesheets().clear();
            themeToggleButton.getScene().getStylesheets().add(getClass().getResource("/css/style-dark.css").toExternalForm());
            themeToggleButton.setText("Light Theme");
            darkTheme = true;
        }
    }

    public static class WatchDebugRow {
        private final String var;
        private final long value;

        public WatchDebugRow(String var, long value) {
            this.var = var;
            this.value = value;
        }

        public String getVariable() {
            return var;
        }

        public long getValue() {
            return value;
        }
    }

    public static class ProgramHistoryRow {
        private final Integer number;
        private final Integer degree;
        private final String input;
        private final Long result;
        private final Integer cycle;

        public ProgramHistoryRow(int number, Integer degree, String input, Long result, Integer cycle) {
            this.number = number;
            this.degree = degree;
            this.input = input;
            this.result = result;
            this.cycle = cycle;
        }
        public Integer getNumber() {
            return number;
        }

        public Integer getDegree() {
            return degree;
        }

        public String getInput() {
            return input;
        }

        public Long getResult() {
            return result;
        }

        public Integer getCycle() {
            return cycle;
        }
    }

    public static class InstructionBaseRow {
        private final Integer number;
        private final String type;
        private final String label;
        private final String instruction;
        private final Integer cycle;

        public InstructionBaseRow(int number, String type, String label, String instruction, int cycle) {
            this.number = number;
            this.type = type;
            this.label = label;
            this.instruction = instruction;
            this.cycle = cycle;
        }

        public Integer getNumber() {
            return number;
        }

        public String getType() {
            return type;
        }

        public String getLabel() {
            return label;
        }

        public String getInstruction() {
            return instruction;
        }

        public Integer getCycle() {
            return cycle;
        }
    }

    public static class InstructionRow extends InstructionBaseRow{

        private final AbstractOpBasic op;
        private boolean breakpoint = false;
        private String strDataInstruction;

        public InstructionRow(int number, String type, String label, String instruction, int cycle, AbstractOpBasic op) {
            super(number, type, label, instruction, cycle);
            this.op = op;
            strDataInstruction = (label.isEmpty() ? "   " : label) + "  " + instruction + " (" + cycle + ")";
        }

        public AbstractOpBasic getOp() {
            return op;
        }

        public boolean getBreakpoint() {
            return breakpoint;
        }
        public void setBreakpoint(boolean bp) {

            this.breakpoint = bp;
        }

        public String getDataInstruction() {
            return strDataInstruction;
        }
    }

    private void setupTreeView() {
        programTree.setCellFactory(tv -> new TreeCell<OpTreeNode>() {
            @Override
            protected void updateItem(OpTreeNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getText());
                    if ("S".equals(item.getType())) {
                        setGraphic(makeColoredCircle("green", "S"));
                    } else if ("B".equals(item.getType())) {
                        setGraphic(makeColoredCircle("blue", "B"));
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

    }

    private StackPane makeColoredCircle(String color, String letter) {
        Circle circle = new Circle(8); // radius = 8px
        circle.setFill(Paint.valueOf(color));
        circle.setStroke(Color.BLACK);
        circle.setStrokeWidth(1);

        Label label = new Label(letter);
        label.getStyleClass().add("bp-label");

        StackPane stack = new StackPane(circle, label);
        stack.setPrefSize(16, 16); // match circle diameter
        return stack;
    }

    private void setupToggleGroup()
    {
        viewToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == tableRadio) {
                instructionTable.setVisible(true);
                instructionTable.setManaged(true);
                programTree.setVisible(false);
                programTree.setManaged(false);
            } else if (newToggle == treeRadio) {
                instructionTable.setVisible(false);
                instructionTable.setManaged(false);
                programTree.setVisible(true);
                programTree.setManaged(true);
            }
        });
    }

    private void setupProgramHistory() {
        colHistoryRunNumber.setCellValueFactory(new PropertyValueFactory<>("number"));
        colHistoryRunDegree.setCellValueFactory(new PropertyValueFactory<>("degree"));
        colHistoryRunInput.setCellValueFactory(new PropertyValueFactory<>("input"));
        colHistoryRunResult.setCellValueFactory(new PropertyValueFactory<>("result"));
        colHistoryRunCycle.setCellValueFactory(new PropertyValueFactory<>("cycle"));
    }

    private void setupProgramTable() {
        colNumber.setCellValueFactory(new PropertyValueFactory<>("number"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colLabel.setCellValueFactory(new PropertyValueFactory<>("label"));
        colInstruction.setCellValueFactory(new PropertyValueFactory<>("instruction"));
        colCycle.setCellValueFactory(new PropertyValueFactory<>("cycle"));

        colBreakpoint.setCellValueFactory(new PropertyValueFactory<>("breakpoint"));
        colBreakpoint.setCellFactory(tc -> {
            TableCell<InstructionRow, Boolean> cell = new TableCell<>() {
                @Override
                protected void updateItem(Boolean bp, boolean empty) {
                    super.updateItem(bp, empty);
                    if (empty || bp == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(bp ? "●" : "");
                        setStyle(bp ? "-fx-text-fill: red; -fx-alignment: CENTER;" : "-fx-alignment: CENTER;");
                    }
                }
            };

            cell.setOnMouseClicked(e -> {
                InstructionRow row = cell.getTableView().getItems().get(cell.getIndex());
                row.setBreakpoint(!row.getBreakpoint());
                cell.getTableView().refresh();
            });

            return cell;
        });

        instructionTable.setRowFactory(tv -> {
            TableRow<InstructionRow> row = new TableRow<>() {
                @Override
                protected void updateItem(InstructionRow item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setStyle(""); // reset style
                    } else {
                        if (getIndex() == currentHighlightedStep) {
                            setStyle("-fx-background-color: lightgreen;"); // step-over
                        } else if (highlightText != null &&
                                (item.getLabel().contains(highlightText) || item.getInstruction().contains(highlightText))) {
                            setStyle("-fx-background-color: #F08650; -fx-text-fill: black;"); // orange highlight
                        } else {
                            setStyle(""); // default
                        }
                    }
                }
            };

            ContextMenu contextMenu = new ContextMenu();
            MenuItem expandAction = new MenuItem("Expand");
            MenuItem collapseAction = new MenuItem("Collapse");
            expandAction.setOnAction(event -> {
                InstructionRow ir = row.getItem();
                if (ir != null) {
                    expandSingle(ir.getOp()); // <-- your function
                }
            });
            collapseAction.setOnAction(event -> {
                InstructionRow ir = row.getItem();
                if (ir != null) {
                    collapseSingle(ir.getOp()); // <-- your function
                }
            });
            contextMenu.getItems().add(expandAction);
            contextMenu.getItems().add(collapseAction);

            // Show context menu only if row is not empty AND type == "S"
            row.setOnContextMenuRequested(event -> {
                InstructionRow ir = row.getItem();
                if (ir != null) {
                    contextMenu.getItems().clear(); // reset items

                    if ("B".equals(ir.getType())) {
                        contextMenu.getItems().add(collapseAction);
                    } else {
                        contextMenu.getItems().addAll(expandAction, collapseAction);
                    }

                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                } else {
                    contextMenu.hide();
                }
            });

            // Add breakpoint toggle on double-click
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    InstructionRow ir = row.getItem();
                    ir.setBreakpoint(!ir.getBreakpoint());
                    instructionTable.refresh(); // redraw
                }
            });

            return row;
        });
    }

    private void setupHistoryTable() {
        colHistoryNumber.setCellValueFactory(new PropertyValueFactory<>("number"));
        colHistoryType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colHistoryLabel.setCellValueFactory(new PropertyValueFactory<>("label"));
        colHistoryInstruction.setCellValueFactory(new PropertyValueFactory<>("instruction"));
        colHistoryCycle.setCellValueFactory(new PropertyValueFactory<>("cycle"));
    }

    private void setupWatchDebugTable() {
        colVar.setCellValueFactory(new PropertyValueFactory<>("variable"));
        colValue.setCellValueFactory(new PropertyValueFactory<>("value"));
    }

    private void setupToolTips()
    {
        Tooltip stopTooltip = new Tooltip("Stop the current \n debugging session");
        stopTooltip.setShowDelay(Duration.ZERO);
        stopTooltip.setShowDuration(Duration.seconds(5));
        stopTooltip.setHideDelay(Duration.ZERO);
        stopButton.setTooltip(stopTooltip);

        Tooltip resumeTooltip = new Tooltip("Resume");
        resumeTooltip.setShowDelay(Duration.ZERO);
        resumeTooltip.setShowDuration(Duration.seconds(5));
        resumeTooltip.setHideDelay(Duration.ZERO);
        resumeButton.setTooltip(resumeTooltip);

        Tooltip stepOverTooltip = new Tooltip("Step over");
        stepOverTooltip.setShowDelay(Duration.ZERO);
        stepOverTooltip.setShowDuration(Duration.seconds(5));
        stepOverTooltip.setHideDelay(Duration.ZERO);
        stepOverButton.setTooltip(stepOverTooltip);

        Tooltip stepBackTooltip = new Tooltip("Step backward");
        stepBackTooltip.setShowDelay(Duration.ZERO);
        stepBackTooltip.setShowDuration(Duration.seconds(5));
        stepBackTooltip.setHideDelay(Duration.ZERO);
        stepBackButton.setTooltip(stepBackTooltip);
    }

    private void populateHighlightComboBox() {
        highlightComboBox.getItems().clear();
        highlightText = "none";
        highlightComboBox.getItems().add("none");
        for (VariableImpl v : programWork.getAllVars()) {
            highlightComboBox.getItems().add(v.getRepresentation());
        }
        for (semulator.label.Label label : programWork.getLabelSet()) {
            highlightComboBox.getItems().add(label.getLabelRepresentation());
        }
    }

    private void populateFuncsSelection()
    {
        funcsComboBox.getItems().clear();
        funcsComboBox.getItems().add("Program");
        if (functions != null) {
            functions.forEach(func -> {
                funcsComboBox.getItems().add(func.getName());
            });
        }
        funcsComboBox.getSelectionModel().selectFirst();
    }

    private void populateInstructionTable(FunctionExecutor program) {

        ObservableList<InstructionRow> data = FXCollections.observableArrayList();
        AbstractOpBasic op;
        int idx = 1;
        program.opListIndexReset();
        while ((op = program.getNextOp()) != null) {
            data.add(new InstructionRow(
                    idx++,
                    op.getType(),
                    op.getLabel().getLabelRepresentation(),
                    op.getRepresentation(),
                    op.getCycles(),
                    op
            ));
        }

        instructionTable.setItems(data);
        populateTreeView(program); // must also run only on FX thread
    }

    private void populateTreeView(FunctionExecutor program) {
        TreeItem<OpTreeNode> rootItem = TreeBuilder.buildTree(program);

        programTree.setRoot(rootItem);
        programTree.setShowRoot(true);
        programTree.getRoot().setExpanded(true);
    }

    private void setRangeDegree(boolean enable)
    {
        if (enable) {
            maxDegree = programWork.getProgramDegree();
            expandLabel.setText("Range degrees (0–" + maxDegree + ")");
            enableHistoryTable(true);
        } else {
            expandLabel.setText("Range degrees");
            enableHistoryTable(false);
        }
    }

    private void enableLoadControls(boolean enable) {
        expandField.setDisable(!enable);
        expandButton.setDisable(!enable);
        collapseButton.setDisable(!enable);
        highlightComboBox.setDisable(!enable);
        funcsComboBox.setDisable(!enable);
        highlightText = "none";
        setRangeDegree(enable);

        expandField.setText("0");
        highlightComboBox.getItems().clear();
        funcsComboBox.getItems().clear();
        instructionTable.getItems().clear();
        instructionTable.getSelectionModel().clearSelection();
        programTree.setRoot(null);
        historyInstrTable.getItems().clear();
        historyInstrTable.getSelectionModel().clearSelection();
    }

    private void enableRunControls(boolean enable) {
        runButton.setDisable(!enable);
        debugButton.setDisable(!enable);
        debugTable.getItems().clear();
        debugTable.getSelectionModel().clearSelection();
        historyRunTable.getItems().clear();
        historyRunTable.getSelectionModel().clearSelection();
        resetBreakPoints();
    }

    private void enableDebugControls(boolean enable) {
        stepOverButton.setDisable(!enable);
        stepBackButton.setDisable(!enable);
        stopButton.setDisable(!enable);
        resumeButton.setDisable(!enable);
    }

    private void resetDebugVars()
    {
        currentHighlightedStep = -1;
        currentStepIndex = 0;
    }

    public void enableHistoryTable(boolean enable) {
        if (historyInstrTable != null) {
            historyInstrTable.setDisable(!enable);
        }
    }

    private void onHighlightSelection(String selected) {
        if (selected != null && !selected.isEmpty()) {
            highlightText = selected;
            Platform.runLater(() -> instructionTable.refresh());
        }
    }

    private void onFuncsSelection(String selected) {
        if (selected == null || selected.equals("Program")) {
            programWork = programCopy;
        } else {
            FunctionExecutor func = programCopy.getFunction(selected);
            if (func != null) {
                programWork = func;
            }
        }

        debugTable.getItems().clear();
        debugTable.getSelectionModel().clearSelection();
        historyInstrTable.getItems().clear();
        historyInstrTable.getSelectionModel().clearSelection();
        resetBreakPoints();

        setRangeDegree(true);
        populateInstructionTable(programWork);
        createRunParameterFields(programWork.getInputVarSize());
        populateHighlightComboBox();
    }

    private void displayFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select XML File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml")
        );

        // Get the current window
        Stage stage = (Stage) filePathField.getScene().getWindow();

        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            filePathField.setText(selectedFile.getAbsolutePath());

            showStatus("File selected: " + selectedFile.getName(), Alert.AlertType.INFORMATION);
            progressBar.setProgress(0.0);
        }
    }
    private void expandControlRoutine()
    {
        historyInstrTable.getItems().clear();
        historyInstrTable.getSelectionModel().clearSelection();
        debugTable.getItems().clear();
        debugTable.getSelectionModel().clearSelection();
        instructionTable.getSelectionModel().clearSelection();
        populateHighlightComboBox();
        resetBreakPoints();
        stopDebugging(false);
    }

    private void resetProgramState()
    {
       // programWork.restoreOriginalVars();
        programWork.resetSnap();
    }

    private void expand(int degree) {
        resetProgramState();
        programWork.expandProgram(1);
        populateInstructionTable(programWork);
        expandField.setText(String.valueOf(degree));
        enableHistoryTable(true);
        expandControlRoutine();
    }

    private void collapse(int degree)
    {
        resetProgramState();
        programWork.collapse();
        populateInstructionTable(programWork);
        expandField.setText(String.valueOf(degree));
        enableHistoryTable(true);
        expandControlRoutine();
    }

    private void expandSingle(AbstractOpBasic op) {
        resetProgramState();
        programWork.expandSingle(op, 1);
        populateInstructionTable(programWork);
        expandControlRoutine();
    }

    private void collapseSingle(AbstractOpBasic op) {
        resetProgramState();
        programWork.collapseSingle(op, 1);
        populateInstructionTable(programWork);
        expandControlRoutine();
    }

    private File validateFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            showAlert("No File Selected", "Please select a file first.", Alert.AlertType.WARNING);
            return null;
        }
        if (!filePath.endsWith(".xml")) {
            showAlert("Invalid File", "Please provide a valid XML file.", Alert.AlertType.WARNING);
            return null;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            showAlert("File Not Found", "The file does not exist:\n" + filePath, Alert.AlertType.ERROR);
            return null;
        }
        return file;
    }

    private TextField createIntegerField() {
        TextField field = new TextField();

        UnaryOperator<Change> filter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("-?\\d*")) { // optional minus + digits only
                return change;
            }
            return null; // reject change
        };

        field.setTextFormatter(new TextFormatter<>(filter));
        return field;
    }

    public void createRunParameterFields(int numParams) {
        // Remove old paramBox if it exists
        if (paramBox != null) {
            runBox.getChildren().remove(paramBox);
        }
        paramFields.clear();

        if (numParams == 0)
            return;

        Label paramLabel = new Label("Enter variables");
        paramLabel.setAlignment(Pos.CENTER);

        // Create a new HBox to hold TextFields
        HBox fieldsBox = new HBox(10);
        fieldsBox.setAlignment(Pos.CENTER);

        for (int i = 0; i < numParams; i++) {
            TextField field = createIntegerField();
            String varName = programWork.getNextVar(i).getRepresentation();
            field.setPromptText(varName);
            field.setPrefWidth(80);
            fieldsBox.getChildren().add(field);
            paramFields.add(field);
        }
        paramBox = new VBox(5, paramLabel, fieldsBox);
        paramBox.setAlignment(Pos.CENTER);

        // Insert the paramBox between Label and Button
        int insertIndex = runBox.getChildren().indexOf(runButtonsBox);
        runBox.getChildren().add(insertIndex, paramBox);
    }

    private Task<FunctionExecutor> createLoadTask(File file) {
        return new Task<>() {
            @Override
            protected FunctionExecutor call() throws Exception {
                updateMessage("Loading file...");
                // Simulate progress (for demo purposes)
                int steps = 200;
                for (int i = 0; i <= steps; i++) {
                    Thread.sleep(10); // small delay to visualize progress
                    updateProgress(i, steps);
                }
                PrintStream originalOut = System.out;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                System.setOut(new PrintStream(baos));
                FunctionExecutor loaded = null;
                try {
                    loaded = factory.loadProgramFromXml(file);
                } finally {
                    System.setOut(originalOut); // restore System.out
                }

                String output = baos.toString().trim();
                if (!output.isEmpty()) {
                    updateMessage(output); // temporarily set task message to show output
                }

                updateProgress(1, 1);
                updateMessage("Program loaded successfully.");

                Thread.sleep(1000);

                return loaded;
            }
        };
    }

    private void bindTaskToUI(Task<FunctionExecutor> task) {
        progressBar.progressProperty().bind(task.progressProperty());
        statusBar.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
                program = task.getValue();
                programCopy = program.myClone();
                ((SprogramImpl)programCopy).calculateQuoteDegree();
                programWork = programCopy;
                functions = ((SprogramImpl)programCopy).getFunctions();
                progressBar.progressProperty().unbind();
                statusBar.textProperty().unbind();
                enableLoadControls(true);
                populateInstructionTable(programWork);
                createRunParameterFields(programWork.getInputVarSize());
                enableRunControls(true);
                populateHighlightComboBox();
                populateFuncsSelection();
                progressBar.setProgress(0.0);
                showStatus("Program loaded successfully.", Alert.AlertType.INFORMATION);
        });

        task.setOnFailed(e -> {
                Throwable ex = task.getException();
                progressBar.progressProperty().unbind();
                statusBar.textProperty().unbind();
                filePathField.clear();
                enableLoadControls(false);
                createRunParameterFields(0);
                enableRunControls(false);
                showAlert("Error", "Error loading program:\n" + ex.getMessage(), Alert.AlertType.ERROR);
                functions = null;
                progressBar.setProgress(0.0);
        });
    }



    private void runTask(Task<FunctionExecutor> task) {
        new Thread(task).start();
    }

    private void resetBreakPoints()
    {
        for (InstructionRow row : instructionTable.getItems()) {
            row.setBreakpoint(false);
        }
    }

    private void stopDebugging(boolean isSetStats) {
        stepOverButton.setDisable(true);
        expandButton.setDisable(false);
        collapseButton.setDisable(false);
        resetDebugVars();
        instructionTable.scrollTo(0);
        instructionTable.refresh();
        enableDebugControls(false);
        if (isSetStats)
            setStatistics();
    }

    private int populateDebugTable(int stepIndex)
    {
        Pair<Integer, TreeMap<VariableImpl, Long>> currentStep = runListMap.get(stepIndex);
        TreeMap<VariableImpl, Long> currentMap = currentStep.getValue();

        ObservableList<WatchDebugRow> data = FXCollections.observableArrayList();

        for( Map.Entry<VariableImpl, Long> entry :currentMap.entrySet()) {
            data.add(new WatchDebugRow(
                    entry.getKey().getRepresentation(),
                    entry.getValue()
            ));
        }
        debugTable.setItems(data);
        return currentStep.getKey();
    }

    private void stepOverRoutine() {
        if(runListMap == null)
        {
            stepOverButton.setDisable(true);
            return;
        }
        if(currentStepIndex >= runListMap.size())
        {
            stopDebugging(true);
            showStatus("Debug finished.", Alert.AlertType.INFORMATION);
            return;
        }

        currentHighlightedStep = populateDebugTable(currentStepIndex);
        currentStepIndex++;
        Platform.runLater(() -> {
            instructionTable.scrollTo(currentHighlightedStep);
            instructionTable.refresh();
        });

        if(currentStepIndex >= runListMap.size())
        {
            stopDebugging(true);
            showStatus("Debug finished.", Alert.AlertType.INFORMATION);
            return;
        }
    }

    private List<Long> getUserVars() {
        List<Long> userVars = new ArrayList<>();
        long value;
        for (TextField field : paramFields) {
            String text = field.getText();
            if (!text.isEmpty())
                value = Long.parseLong(text);
            else
                value = 0;
            userVars.add(value);
        }
        return userVars;
    }

    private void runDebugRoutine()
    {
        List<Long> userVars = getUserVars();
            runListMap = ProgramExecutorImpl.run(programWork, userVars, ((SprogramImpl)programCopy).getFunctions());

            currentStepIndex = 0;
            stepOverButton.setDisable(runListMap == null || runListMap.isEmpty());
            instructionTable.refresh();
            onStepOver();
    }

    private void setStatistics()
    {
        runHistoryCounter++;
        long result = programWork.getVariableValue(VariableImpl.RESULT);
        int cycles = programWork.calculateCycles();
        int degree = Integer.parseInt(expandField.getText().trim());
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (TextField field : paramFields) {
            String text = field.getText();
            String varName = programWork.getNextVar(i++).getRepresentation();
            long value = !text.isEmpty() ? Long.parseLong(text) : 0;
            sb.append(varName).append(" = ").append(value).append("   ");
        }

        historyRunData.add(new ProgramHistoryRow(
                runHistoryCounter,
                degree,
                sb.toString(),
                result,
                cycles
        ));
    }

    private void runRoutine()
    {
        List<Long> userVars = getUserVars();
        stopDebugging(false);

        runListMap = ProgramExecutorImpl.run(programWork, userVars, ((SprogramImpl)programCopy).getFunctions());

        if (runListMap != null && !runListMap.isEmpty()) {
            // Populate debug table for the last step
            populateDebugTable(runListMap.size() - 1);
        }
        // Update statistics
        setStatistics();
    }

    private boolean checkAndConfirmParams() {
        boolean hasEmpty = paramFields.stream().anyMatch(f -> f.getText().isEmpty());

        if (!hasEmpty) {
            return true; // no empty, go ahead
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Empty Parameters");
        alert.setHeaderText("Some parameter fields are empty.");
        Label content = new Label("Empty fields will be treated as 0.\nDo you want to continue?");
        content.setWrapText(true);
        content.setAlignment(Pos.CENTER);

        alert.getDialogPane().setContent(content);

        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(yesButton, noButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == yesButton) {
            for (TextField field : paramFields) {
                if (field.getText().trim().isEmpty()) {
                    field.setText("0");
                }
            }
            return true; // Continue
        } else {
            return false; // Cancel
        }
    }

    private void showHistory(AbstractOpBasic op) {

        ObservableList<InstructionBaseRow> historyData = FXCollections.observableArrayList();

        historyInstrTable.getItems().clear();
        historyInstrTable.getSelectionModel().clearSelection();

        int idx = 1;
        AbstractOpBasic currOp = op;
        while(currOp != null) {
            historyData.add(new InstructionBaseRow(
                    idx++,
                    currOp.getType(),
                    currOp.getLabel().getLabelRepresentation(),
                    currOp.getRepresentation(),
                    currOp.getCycles()
            ));
            currOp = currOp.getParent();
        }

        historyInstrTable.setItems(historyData);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        showStatus(message, type);
    }

    private void showStatus(String message, Alert.AlertType type) {
        statusBar.setText(message);
        if (type == Alert.AlertType.ERROR) {
            statusBar.setStyle("-fx-text-fill: red; -fx-border-color: lightgray; -fx-padding: 5;");
        } else if (type == Alert.AlertType.WARNING) {
            statusBar.setStyle("-fx-text-fill: orange; -fx-border-color: lightgray; -fx-padding: 5;");
        } else {
            statusBar.setStyle("-fx-text-fill: green; -fx-border-color: lightgray; -fx-padding: 5;");
        }

        PauseTransition pause = new PauseTransition(Duration.seconds(10));
        pause.setOnFinished(event -> Platform.runLater(() -> {
            if (statusBar.textProperty().isBound()) {
                statusBar.textProperty().unbind();
            }
            statusBar.setText("");
        }));


        pause.play();
    }
}
