package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ProjectBuildEnvironment {

    public ProjectBuildEnvironment(String projectName, String workingDir, Integer lastCommitsCnt) {
        this.projectName = projectName;
        this.workingDir = workingDir;
        if (lastCommitsCnt != null) {
            this.lastCommits = lastCommitsCnt.intValue();
        }
    }
    private final String projectName;
    private final String workingDir;

    private int lastCommits = 100;

    public void runAllBuild() {

    }



    private List<String> getLastCommits() {
        List<String> lastCommits = new ArrayList<>();
        // 1. run 'git log --pretty="%h" '
        try {
            Process commitsFetchProcess = Main.executeCommands(
                    "project_name_commits_fetch",
                    workingDir,
                    // String.format doesn't let me work with '%' normally...
                    "git log -" + lastCommits + " --pretty=\"%h\""
            );
            var processOutput = commitsFetchProcess.getInputStream();
            var reader = new BufferedReader(new InputStreamReader(processOutput));
            String commitHash = "";
            while ((commitHash = reader.readLine()) != null) {
                lastCommits.add(commitHash);
            }
        } catch (Exception e) {
            return null;
        }
        // 2. parse output
        return lastCommits;
    }

}
