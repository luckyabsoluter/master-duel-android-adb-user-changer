package opensource.master_duel_android_adb_user_changer;

public class CommandResult {
    private final int exitCode;
    private final String stdout;
    private final String stderr;
    private final String commandLine;

    public CommandResult(int exitCode, String stdout, String stderr, String commandLine) {
        this.exitCode = exitCode;
        this.stdout = stdout == null ? "" : stdout;
        this.stderr = stderr == null ? "" : stderr;
        this.commandLine = commandLine == null ? "" : commandLine;
    }

    public static CommandResult success(String message) {
        return new CommandResult(0, message, "", "");
    }

    public static CommandResult failure(String stdout, String stderr) {
        return new CommandResult(1, stdout, stderr, "");
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public boolean isSuccess() {
        return exitCode == 0;
    }

    public String toDisplayString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Command: ").append(commandLine.isBlank() ? "(internal)" : commandLine).append("\n");
        builder.append("Exit: ").append(exitCode).append("\n");
        if (!stdout.isBlank()) {
            builder.append("Stdout:\n").append(stdout.strip()).append("\n");
        }
        if (!stderr.isBlank()) {
            builder.append("Stderr:\n").append(stderr.strip()).append("\n");
        }
        return builder.toString().trim();
    }
}
