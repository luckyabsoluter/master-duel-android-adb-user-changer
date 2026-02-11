package opensource.master_duel_android_adb_user_changer;

public class UserProfile {
    private final String folderName;
    private final String alias;
    private final boolean active;
    private final String metadataPath;
    private final String warning;

    public UserProfile(String folderName, String alias, boolean active, String metadataPath, String warning) {
        this.folderName = folderName;
        this.alias = alias;
        this.active = active;
        this.metadataPath = metadataPath;
        this.warning = warning;
    }

    public static UserProfile error(String message) {
        return new UserProfile("(error)", "(error)", false, "", message);
    }

    public String getFolderName() {
        return folderName;
    }

    public String getAlias() {
        return alias;
    }

    public boolean isActive() {
        return active;
    }

    public String getMetadataPath() {
        return metadataPath;
    }

    public String getWarning() {
        return warning;
    }

    public String toDetailedString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Folder: ").append(folderName).append("\n");
        builder.append("Alias: ").append(alias).append("\n");
        builder.append("Active: ").append(active).append("\n");
        builder.append("Metadata: ").append(metadataPath).append("\n");
        if (warning != null && !warning.isBlank()) {
            builder.append("Warning: ").append(warning);
        }
        return builder.toString();
    }
}
