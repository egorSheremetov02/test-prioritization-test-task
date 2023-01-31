package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ProjectBuildEnvironment {

    public ProjectBuildEnvironment(String projectName, String workingDir, Integer lastCommitsCnt) {
        this.projectName = projectName;
        this.workingDir = workingDir;
        if (lastCommitsCnt != null) {
            this.lastCommitsCnt = lastCommitsCnt.intValue();
        }
        for (int i = 0; i < this.lastCommitsCnt; ++i) {
            this.isBuildSuccessful.add(false);
            this.timeTakenToBuild.add(0L);
        }
        this.fetchLastCommitsHashes();
    }
    private final String projectName;
    private final String workingDir;

    private final ArrayList<Boolean> isBuildSuccessful = new ArrayList<>();
    private final ArrayList<Long> timeTakenToBuild = new ArrayList<>();

    private final ArrayList<String> lastCommits = new ArrayList<>();

    private int lastCommitsCnt = 100;

    public void buildAll(String buildCmd) {
        for (int idx = 0; idx < lastCommits.size(); ++idx) {
            this.buildVersion(idx, buildCmd);
        }
    }



    // TODO: make private again
    public List<String> fetchLastCommitsHashes() {
        // 1. run 'git log -10 --pretty="%h" '
        try {
            Process commitsFetchProcess = Main.executeCommands(
                    "project_name_commits_fetch",
                    workingDir,
                    // String.format doesn't let me work with '%' normally...
                    "git log -" + this.lastCommitsCnt + " --pretty=\"%h\""
            );
            // 2. parse output
            var processOutput = commitsFetchProcess.getInputStream();
            var reader = new BufferedReader(new InputStreamReader(processOutput));
            String commitHash = "";
            while ((commitHash = reader.readLine()) != null) {
                lastCommits.add(commitHash);
            }
        } catch (Exception e) {
            return null;
        }
        return lastCommits;
    }

    public void printBuildStatistics() {
        int successfulBuildsCnt = 0;
        long overallTime = 0;
        long timeTakenToBuildSuccessfully = 0;
        for (int i = 0; i < lastCommits.size(); ++i) {
            if (isBuildSuccessful.get(i)) {
                ++successfulBuildsCnt;
                timeTakenToBuildSuccessfully += timeTakenToBuild.get(i);
            }
            overallTime += timeTakenToBuild.get(i);
        }
        System.out.println("Here is statistics for build of " + projectName + " project:");
        System.out.println("Successful: " + successfulBuildsCnt + " out of " + lastCommits.size());
        System.out.println("Overall build time: " + overallTime + " ms");
        System.out.println("Time it took to finish successful builds: " + timeTakenToBuildSuccessfully + " ms");
    }

    public void buildVersion(int idx, String buildCmd) {
        String commitHash = lastCommits.get(idx);
        String commandName = "building_project_version_" + projectName + "_" + commitHash;
        String dirName = "version_" + idx;

        Instant start = Instant.now();
        try {
            Main.executeCommands(
                    commandName,
                    workingDir,
                    "git checkout " + commitHash,
                    "mkdir " + dirName,
                    "cd " + dirName,
                    buildCmd
            );
        } catch (Exception e) {
            System.err.println("Couldn't build version with commit hash " + commitHash);
            isBuildSuccessful.set(idx, false);
            return;
        }
        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        isBuildSuccessful.set(idx, false);
        timeTakenToBuild.set(idx, timeElapsed);
    }

}
