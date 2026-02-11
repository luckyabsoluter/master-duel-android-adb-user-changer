package opensource.master_duel_android_adb_user_changer;

import javafx.beans.property.StringProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AdbService {
    private static final String MASTER_DUEL_FILES = "/data/data/jp.konami.masterduel/files";
    private static final String METADATA_FILE = "master-duel-android-adb-user-changer-metadata.properties";

    private final StringProperty adbPath;
    private final MetadataSerializer metadataSerializer = new MetadataSerializer();

    public AdbService(StringProperty adbPath) {
        this.adbPath = adbPath;
    }

    public CommandResult getAdbVersion() {
        return client().run(List.of("version"));
    }

    public List<DeviceInfo> listDevices() {
        CommandResult result = client().run(List.of("devices", "-l"));
        List<DeviceInfo> list = new ArrayList<>();
        if (!result.isSuccess()) {
            return list;
        }

        String[] lines = result.getStdout().split("\\R");
        for (String line : lines) {
            if (line.startsWith("List of devices") || line.isBlank()) {
                continue;
            }
            String[] parts = line.trim().split("\\s+");
            if (parts.length >= 2) {
                list.add(new DeviceInfo(parts[0], parts[1], line.trim()));
            }
        }
        return list;
    }

    public CommandResult checkRoot(String serial) {
        return runSu(serial, "id");
    }

    public CommandResult getDeviceInfo(String serial) {
        List<DeviceProp> props = List.of(
                new DeviceProp("Model", "ro.product.model"),
                new DeviceProp("Manufacturer", "ro.product.manufacturer"),
                new DeviceProp("Android", "ro.build.version.release"),
                new DeviceProp("SDK", "ro.build.version.sdk"),
                new DeviceProp("Build", "ro.build.display.id")
        );

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        int exitCode = 0;

        for (DeviceProp prop : props) {
            CommandResult result = runShell(serial, "getprop", prop.key());
            String value = result.isSuccess() ? result.getStdout().strip() : "(error)";
            if (!result.isSuccess()) {
                exitCode = 1;
                if (!stderr.isEmpty()) {
                    stderr.append("\n");
                }
                stderr.append(prop.label()).append(" -> ").append(result.toDisplayString());
            }
            stdout.append(prop.label()).append(": ").append(value).append("\n");
        }

        return new CommandResult(exitCode, stdout.toString(), stderr.toString(), "adb shell getprop (multi)");
    }

    public List<UserProfile> listUsers(String serial) {
        CommandResult rootCheck = checkRoot(serial);
        if (!rootCheck.isSuccess() || !rootCheck.getStdout().contains("uid=0")) {
            return List.of(UserProfile.error("Root access not available"));
        }

        CommandResult listResult = runSu(serial, "ls -1 " + MASTER_DUEL_FILES);
        if (!listResult.isSuccess()) {
            return List.of(UserProfile.error(listResult.toDisplayString()));
        }

        List<UserProfile> profiles = new ArrayList<>();
        String[] entries = listResult.getStdout().split("\\R");
        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.startsWith("persistent")) {
                profiles.add(loadProfile(serial, trimmed));
            }
        }
        return profiles;
    }

    public CommandResult switchUser(String serial, String targetFolder) {
        if ("persistent".equals(targetFolder)) {
            return CommandResult.success("Already active.");
        }

        CommandResult rootCheck = checkRoot(serial);
        if (!rootCheck.isSuccess() || !rootCheck.getStdout().contains("uid=0")) {
            return CommandResult.failure("Root access not available", rootCheck.getStdout());
        }

        String temp = "persistent__tmp__" + Instant.now().toEpochMilli();

        CommandResult step1 = runSu(serial, "mv " + MASTER_DUEL_FILES + "/persistent " + MASTER_DUEL_FILES + "/" + temp);
        if (!step1.isSuccess()) {
            return step1;
        }

        CommandResult step2 = runSu(serial, "mv " + MASTER_DUEL_FILES + "/" + targetFolder + " " + MASTER_DUEL_FILES + "/persistent");
        if (!step2.isSuccess()) {
            runSu(serial, "mv " + MASTER_DUEL_FILES + "/" + temp + " " + MASTER_DUEL_FILES + "/persistent");
            return step2;
        }

        CommandResult step3 = runSu(serial, "mv " + MASTER_DUEL_FILES + "/" + temp + " " + MASTER_DUEL_FILES + "/" + targetFolder);
        if (!step3.isSuccess()) {
            return step3;
        }

        updateLastSwitch(serial, "persistent");
        return CommandResult.success("Switch completed");
    }

    public CommandResult updateAlias(String serial, String folder, String alias) {
        return writeMetadata(serial, folder, alias, Instant.now().toString(), Instant.now().toString());
    }

    public CommandResult archivePersistent(String serial, String newFolder) {
        if (newFolder == null || newFolder.isBlank()) {
            return CommandResult.failure("Folder name is required.", "");
        }
        if ("persistent".equals(newFolder)) {
            return CommandResult.failure("Folder name must not be persistent.", "");
        }

        CommandResult rootCheck = checkRoot(serial);
        if (!rootCheck.isSuccess() || !rootCheck.getStdout().contains("uid=0")) {
            return CommandResult.failure("Root access not available", rootCheck.getStdout());
        }

        CommandResult listResult = runSu(serial, "ls -1 " + MASTER_DUEL_FILES);
        if (!listResult.isSuccess()) {
            return listResult;
        }
        for (String entry : listResult.getStdout().split("\\R")) {
            if (newFolder.equals(entry.trim())) {
                return CommandResult.failure("Folder already exists: " + newFolder, "");
            }
        }

        return runSu(serial,
                "mv " + MASTER_DUEL_FILES + "/persistent " + MASTER_DUEL_FILES + "/" + newFolder);
    }

    public CommandResult switchUserAndArchiveActive(String serial, String targetFolder, String archiveFolder) {
        if (targetFolder == null || targetFolder.isBlank()) {
            return CommandResult.failure("Target folder is required.", "");
        }
        if ("persistent".equals(targetFolder)) {
            return CommandResult.failure("Target folder must not be persistent.", "");
        }
        if (archiveFolder == null || archiveFolder.isBlank()) {
            return CommandResult.failure("Archive folder is required.", "");
        }
        if ("persistent".equals(archiveFolder)) {
            return CommandResult.failure("Archive folder must not be persistent.", "");
        }
        if (archiveFolder.equals(targetFolder)) {
            return CommandResult.failure("Archive folder must differ from target.", "");
        }

        CommandResult rootCheck = checkRoot(serial);
        if (!rootCheck.isSuccess() || !rootCheck.getStdout().contains("uid=0")) {
            return CommandResult.failure("Root access not available", rootCheck.getStdout());
        }

        CommandResult listResult = runSu(serial, "ls -1 " + MASTER_DUEL_FILES);
        if (!listResult.isSuccess()) {
            return listResult;
        }
        boolean targetExists = false;
        boolean persistentExists = false;
        boolean archiveExists = false;
        for (String entry : listResult.getStdout().split("\\R")) {
            String name = entry.trim();
            if (archiveFolder.equals(name)) {
                archiveExists = true;
            }
            if (targetFolder.equals(name)) {
                targetExists = true;
            }
            if ("persistent".equals(name)) {
                persistentExists = true;
            }
        }
        if (!targetExists) {
            return CommandResult.failure("Target folder not found: " + targetFolder, "");
        }
        if (persistentExists && archiveExists) {
            return CommandResult.failure("Archive folder already exists: " + archiveFolder, "");
        }

        if (!persistentExists) {
            CommandResult promote = runSu(serial,
                    "mv " + MASTER_DUEL_FILES + "/" + targetFolder + " " + MASTER_DUEL_FILES + "/persistent");
            if (!promote.isSuccess()) {
                return promote;
            }
            updateLastSwitch(serial, "persistent");
            return CommandResult.success("Switch completed");
        }

        CommandResult step1 = runSu(serial,
                "mv " + MASTER_DUEL_FILES + "/persistent " + MASTER_DUEL_FILES + "/" + archiveFolder);
        if (!step1.isSuccess()) {
            return step1;
        }

        CommandResult step2 = runSu(serial,
                "mv " + MASTER_DUEL_FILES + "/" + targetFolder + " " + MASTER_DUEL_FILES + "/persistent");
        if (!step2.isSuccess()) {
            runSu(serial,
                "mv " + MASTER_DUEL_FILES + "/" + archiveFolder + " " + MASTER_DUEL_FILES + "/persistent");
            return step2;
        }

        updateLastSwitch(serial, "persistent");
        return CommandResult.success("Switch completed");
    }

    private void updateLastSwitch(String serial, String folder) {
        String alias = readAlias(serial, folder);
        writeMetadata(serial, folder, alias, null, Instant.now().toString());
    }

    private UserProfile loadProfile(String serial, String folder) {
        String path = MASTER_DUEL_FILES + "/" + folder + "/" + METADATA_FILE;
        CommandResult result = runSu(serial, "cat " + ShellEscaper.quote(path));
        Properties properties = new Properties();
        String alias = folder;
        if (result.isSuccess()) {
            try {
                properties.load(new java.io.StringReader(result.getStdout()));
                alias = properties.getProperty("alias", folder);
            } catch (Exception ignored) {
                alias = folder;
            }
        }
        boolean active = "persistent".equals(folder);
        return new UserProfile(folder, alias, active, path, result.isSuccess() ? "" : result.getStderr());
    }

    private String readAlias(String serial, String folder) {
        String path = MASTER_DUEL_FILES + "/" + folder + "/" + METADATA_FILE;
        CommandResult result = runSu(serial, "cat " + ShellEscaper.quote(path));
        if (!result.isSuccess()) {
            return folder;
        }
        Properties properties = new Properties();
        try {
            properties.load(new java.io.StringReader(result.getStdout()));
            return properties.getProperty("alias", folder);
        } catch (Exception ignored) {
            return folder;
        }
    }

    private CommandResult writeMetadata(String serial, String folder, String alias, String lastUpdatedUtc, String lastSwitchUtc) {
        String path = MASTER_DUEL_FILES + "/" + folder + "/" + METADATA_FILE;
        Properties properties = new Properties();
        properties.setProperty("alias", alias);
        if (lastUpdatedUtc != null) {
            properties.setProperty("lastUpdatedUtc", lastUpdatedUtc);
        }
        if (lastSwitchUtc != null) {
            properties.setProperty("lastSwitchUtc", lastSwitchUtc);
        }

        byte[] payload = metadataSerializer.serialize(properties);
        AdbFileWriter writer = new AdbFileWriter(adbPath.get());
        return writer.writeAsRoot(serial, path, payload);
    }

    private AdbClient client() {
        return new AdbClient(adbPath.get());
    }

    private CommandResult runSu(String serial, String command) {
        return runShell(serial, "su", "-c", command);
    }

    private CommandResult runShell(String serial, String... shellArgs) {
        List<String> args = new ArrayList<>();
        args.add("-s");
        args.add(serial);
        args.add("shell");
        for (String arg : shellArgs) {
            args.add(arg);
        }
        return client().run(args);
    }

    private record DeviceProp(String label, String key) {
    }
}
