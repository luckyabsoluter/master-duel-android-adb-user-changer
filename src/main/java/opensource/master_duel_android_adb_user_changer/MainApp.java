package opensource.master_duel_android_adb_user_changer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class MainApp extends Application {
    private final String appVersion = resolveAppVersion();
    private final SimpleStringProperty adbPath = new SimpleStringProperty("adb");
    private final AdbService adbService = new AdbService(adbPath);

    private final ObservableList<DeviceInfo> devices = FXCollections.observableArrayList();
    private final ObservableList<UserProfile> users = FXCollections.observableArrayList();

    private final TextArea logArea = new TextArea();
    private final TextArea deviceDetails = new TextArea();
    private final TextArea userDetails = new TextArea();

    private final ComboBox<DeviceInfo> deviceCombo = new ComboBox<>(devices);
    private final TableView<UserProfile> userTable = new TableView<>(users);

    private final Label adbVersionLabel = new Label("ADB version: unknown");

    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(buildRoot(), 1100, 720);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        primaryStage.setTitle("Master Duel Android ADB User Changer v" + appVersion);
        primaryStage.setScene(scene);
        primaryStage.show();

        refreshAdbVersion();
        refreshDevices();
    }

    private BorderPane buildRoot() {
        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setCenter(buildTabs());
        return root;
    }

    private Node buildHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Master Duel Android ADB User Changer");
        title.getStyleClass().add("section-title");

        Label versionLabel = new Label("v" + appVersion);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, versionLabel, spacer);
        return header;
    }

    private TabPane buildTabs() {
        TabPane tabs = new TabPane();
        tabs.getTabs().add(buildDevicesTab());
        tabs.getTabs().add(buildUsersTab());
        tabs.getTabs().add(buildLogsTab());
        tabs.getTabs().add(buildAdbTab());
        tabs.getTabs().add(buildAboutTab());
        return tabs;
    }

    private Tab buildAdbTab() {
        Tab tab = new Tab("ADB");
        tab.setClosable(false);

        VBox box = new VBox(12);
        box.setPadding(new Insets(12));

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        TextField adbField = new TextField();
        adbField.textProperty().bindBidirectional(adbPath);
        adbField.setPrefColumnCount(28);

        Button adbVersionButton = new Button("Check ADB");
        adbVersionButton.setOnAction(event -> refreshAdbVersion());

        row.getChildren().addAll(new Label("ADB Path:"), adbField, adbVersionButton, adbVersionLabel);

        box.getChildren().addAll(row);
        tab.setContent(box);
        return tab;
    }

    private Tab buildDevicesTab() {
        Tab tab = new Tab("Devices");
        tab.setClosable(false);

        VBox box = new VBox(12);
        box.setPadding(new Insets(12));

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Button refreshButton = new Button("Refresh Devices");
        refreshButton.setOnAction(event -> refreshDevices());

        Button rootCheckButton = new Button("Check Root");
        rootCheckButton.setOnAction(event -> checkRoot());

        Button infoButton = new Button("Load Device Info");
        infoButton.setOnAction(event -> loadDeviceInfo());

        deviceCombo.setPromptText("Select device");
        deviceCombo.setPrefWidth(320);

        row.getChildren().addAll(refreshButton, deviceCombo, rootCheckButton, infoButton);

        deviceDetails.setEditable(false);
        deviceDetails.setPrefRowCount(18);
        deviceDetails.setMinHeight(0);
        VBox.setVgrow(deviceDetails, Priority.ALWAYS);

        box.getChildren().addAll(row, new Label("Device Details"), deviceDetails);
        tab.setContent(box);
        return tab;
    }

    private Tab buildUsersTab() {
        Tab tab = new Tab("Users");
        tab.setClosable(false);

        VBox box = new VBox(12);
        box.setPadding(new Insets(12));

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Button refreshButton = new Button("Refresh Users");
        refreshButton.setOnAction(event -> refreshUsers());

        Button switchButton = new Button("Activate Selected");
        switchButton.setOnAction(event -> switchToSelectedUser());

        Button renameButton = new Button("Set Alias");
        renameButton.setOnAction(event -> setAlias());

        Button newUserButton = new Button("Deactivate Current User");
        newUserButton.setOnAction(event -> deactivateCurrentUser());

        row.getChildren().addAll(refreshButton, switchButton, renameButton, newUserButton);

        TableColumn<UserProfile, Boolean> activeCol = new TableColumn<>("Active");
        activeCol.setCellValueFactory(cellData -> new SimpleBooleanProperty(cellData.getValue().isActive()));
        activeCol.setCellFactory(column -> new javafx.scene.control.cell.CheckBoxTableCell<>());
        activeCol.setPrefWidth(70);

        TableColumn<UserProfile, String> folderCol = new TableColumn<>("Folder");
        folderCol.setCellValueFactory(new PropertyValueFactory<>("folderName"));
        folderCol.setPrefWidth(240);

        TableColumn<UserProfile, String> aliasCol = new TableColumn<>("Alias");
        aliasCol.setCellValueFactory(new PropertyValueFactory<>("alias"));
        aliasCol.setPrefWidth(320);

        userTable.getColumns().addAll(activeCol, folderCol, aliasCol);
        userTable.setPrefHeight(360);
        userTable.setMinHeight(0);
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                userDetails.setText(newValue.toDetailedString());
            }
        });

        userDetails.setEditable(false);
        userDetails.setPrefRowCount(10);
        userDetails.setPrefHeight(180);
        userDetails.setMinHeight(0);

        VBox userDetailsBox = new VBox(8, new Label("User Details"), userDetails);
        VBox.setVgrow(userDetails, Priority.ALWAYS);

        SplitPane detailsSplit = new SplitPane(userTable, userDetailsBox);
        detailsSplit.setOrientation(Orientation.VERTICAL);
        detailsSplit.setDividerPositions(0.7);
        detailsSplit.setMinHeight(0);

        box.getChildren().addAll(row, detailsSplit);
        tab.setContent(box);
        return tab;
    }

    private Tab buildLogsTab() {
        Tab tab = new Tab("Logs");
        tab.setClosable(false);

        VBox box = new VBox(8);
        box.setPadding(new Insets(12));

        Button clearButton = new Button("Clear");
        clearButton.setOnAction(event -> logArea.clear());

        logArea.getStyleClass().add("log-area");
        logArea.setEditable(false);
        logArea.setMinHeight(0);
        VBox.setVgrow(logArea, Priority.ALWAYS);

        box.getChildren().addAll(clearButton, logArea);
        tab.setContent(box);
        return tab;
    }

    private Tab buildAboutTab() {
        Tab tab = new Tab("About");
        tab.setClosable(false);

        VBox box = new VBox(8);
        box.setPadding(new Insets(12));

        String repoUrl = "https://github.com/luckyabsoluter/master-duel-android-adb-user-changer";

        TextArea info = new TextArea("Switch Master Duel users by renaming persistent folders via ADB (root required).\n" +
                "Metadata is stored in each persistent folder as master-duel-android-adb-user-changer-metadata.properties.\n" +
                "Version: " + appVersion + "\n" +
                "GitHub: " + repoUrl);
        info.setEditable(false);
        info.setWrapText(true);
        info.setPrefRowCount(6);

        Hyperlink githubLink = new Hyperlink("[Github]");
        githubLink.setOnAction(event -> getHostServices().showDocument(repoUrl));
        box.getChildren().addAll(info, githubLink);
        tab.setContent(box);
        return tab;
    }

    private Optional<DeviceInfo> getSelectedDevice() {
        DeviceInfo device = deviceCombo.getSelectionModel().getSelectedItem();
        if (device == null) {
            showAlert("Select a device first.");
            return Optional.empty();
        }
        return Optional.of(device);
    }

    private void refreshAdbVersion() {
        Task<CommandResult> task = new Task<>() {
            @Override
            protected CommandResult call() {
                return adbService.getAdbVersion();
            }
        };
        task.setOnSucceeded(event -> {
            CommandResult result = task.getValue();
            logCommand("adb version", result);
            if (result.isSuccess()) {
                adbVersionLabel.setText("ADB version: " + result.getStdout().strip());
            } else {
                adbVersionLabel.setText("ADB version: error");
                showAlert("ADB version check failed.\n" + result.toDisplayString());
            }
        });
        runTask(task);
    }

    private void refreshDevices() {
        Task<List<DeviceInfo>> task = new Task<>() {
            @Override
            protected List<DeviceInfo> call() {
                return adbService.listDevices();
            }
        };
        task.setOnSucceeded(event -> {
            devices.setAll(task.getValue());
            if (!devices.isEmpty()) {
                deviceCombo.getSelectionModel().select(0);
            }
        });
        runTask(task);
    }

    private void checkRoot() {
        getSelectedDevice().ifPresent(device -> {
            Task<CommandResult> task = new Task<>() {
                @Override
                protected CommandResult call() {
                    return adbService.checkRoot(device.getSerial());
                }
            };
            task.setOnSucceeded(event -> {
                CommandResult result = task.getValue();
                logCommand("su -c id", result);
                if (result.isSuccess() && result.getStdout().contains("uid=0")) {
                    showAlert("Root access confirmed.");
                } else {
                    showAlert("Root check failed.\n" + result.toDisplayString());
                }
            });
            runTask(task);
        });
    }

    private void loadDeviceInfo() {
        getSelectedDevice().ifPresent(device -> {
            Task<CommandResult> task = new Task<>() {
                @Override
                protected CommandResult call() {
                    return adbService.getDeviceInfo(device.getSerial());
                }
            };
            task.setOnSucceeded(event -> {
                CommandResult result = task.getValue();
                logCommand("getprop", result);
                if (result.isSuccess()) {
                    deviceDetails.setText(result.getStdout().trim());
                } else {
                    deviceDetails.setText(result.toDisplayString());
                }
            });
            runTask(task);
        });
    }

    private void refreshUsers() {
        getSelectedDevice().ifPresent(device -> {
            Task<List<UserProfile>> task = new Task<>() {
                @Override
                protected List<UserProfile> call() {
                    return adbService.listUsers(device.getSerial());
                }
            };
            task.setOnSucceeded(event -> {
                users.setAll(task.getValue());
                if (!users.isEmpty()) {
                    userTable.getSelectionModel().select(0);
                }
            });
            runTask(task);
        });
    }

    private void switchToSelectedUser() {
        getSelectedDevice().ifPresent(device -> {
            UserProfile selected = userTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Select a user first.");
                return;
            }
            if ("(error)".equals(selected.getFolderName())) {
                showAlert("Cannot switch due to an error in user listing.\nCheck logs for details.");
                return;
            }
            if (selected.isActive()) {
                showAlert("Selected user is already active.");
                return;
            }

            String archiveName = generateArchiveName();
            Task<CommandResult> task = new Task<>() {
                @Override
                protected CommandResult call() {
                    return adbService.switchUserAndArchiveActive(
                            device.getSerial(),
                            selected.getFolderName(),
                            archiveName
                    );
                }
            };
            task.setOnSucceeded(event -> {
                CommandResult result = task.getValue();
                logCommand("activate selected", result);
                if (result.isSuccess()) {
                    showAlert("User activated.\nPrevious active renamed to " + archiveName + ".");
                    refreshUsers();
                } else {
                    showAlert("Activation failed.\n" + result.toDisplayString());
                }
            });
            runTask(task);
        });
    }

    private void setAlias() {
        getSelectedDevice().ifPresent(device -> {
            UserProfile selected = userTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Select a user first.");
                return;
            }

            TextInputDialog dialog = new TextInputDialog(selected.getAlias());
            dialog.setTitle("Set Alias");
            dialog.setHeaderText("Alias for " + selected.getFolderName());
            dialog.setContentText("Alias:");

            dialog.showAndWait().ifPresent(alias -> {
                Task<CommandResult> task = new Task<>() {
                    @Override
                    protected CommandResult call() {
                        return adbService.updateAlias(device.getSerial(), selected.getFolderName(), alias);
                    }
                };
                task.setOnSucceeded(event -> {
                    CommandResult result = task.getValue();
                    logCommand("set alias", result);
                    if (result.isSuccess()) {
                        refreshUsers();
                    } else {
                        showAlert("Alias update failed.\n" + result.toDisplayString());
                    }
                });
                runTask(task);
            });
        });
    }

    private void deactivateCurrentUser() {
        getSelectedDevice().ifPresent(device -> {
            Optional<UserProfile> activeUser = users.stream()
                    .filter(UserProfile::isActive)
                    .findFirst();
            if (activeUser.isEmpty()) {
                showAlert("No active user found.");
                return;
            }

            String archiveName = generateArchiveName();

            Task<CommandResult> task = new Task<>() {
                @Override
                protected CommandResult call() {
                    return adbService.archivePersistent(device.getSerial(), archiveName);
                }
            };
            task.setOnSucceeded(event -> {
                CommandResult result = task.getValue();
                logCommand("deactivate current user", result);
                if (result.isSuccess()) {
                    showAlert("Current user deactivated.\nRenamed to " + archiveName + ".\nLaunch the app to create a new user.");
                    refreshUsers();
                } else {
                    showAlert("Deactivation failed.\n" + result.toDisplayString());
                }
            });
            runTask(task);
        });
    }

    private String generateArchiveName() {
        String base = "persistent_archived_" + Instant.now().toEpochMilli();
        String candidate = base;
        int index = 1;
        while (folderExists(candidate)) {
            candidate = base + "_" + index;
            index++;
        }
        return candidate;
    }

    private boolean folderExists(String folderName) {
        for (UserProfile profile : users) {
            if (folderName.equals(profile.getFolderName())) {
                return true;
            }
        }
        return false;
    }

    private void runTask(Task<?> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void logCommand(String label, CommandResult result) {
        String timestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());

        logArea.appendText("[" + timestamp + "] " + label + "\n");
        logArea.appendText(result.toDisplayString() + "\n\n");
    }

    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Master Duel Android ADB User Changer");
            alert.setHeaderText(null);
            TextArea messageArea = new TextArea(message);
            messageArea.setEditable(false);
            messageArea.setWrapText(true);
            messageArea.setPrefRowCount(6);
            messageArea.setPrefColumnCount(60);
            alert.getDialogPane().setContent(messageArea);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

    private String resolveAppVersion() {
        return (BuildConfig.VERSION == null || BuildConfig.VERSION.isBlank()) ? "dev" : BuildConfig.VERSION;
    }
}
