package org.example;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static String morphia = "morphia";
    public static String astminer = "astminer";

    public static final Map<String, String> repoNameToRepoUrl = Map.of(
            Main.morphia, Links.morphiaGithubSshLink,
            Main.astminer, Links.astminerGithubSshLink
    );

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Specify directory where you want to build repositories as an argument for your");
            return;
        }
        String buildRootDirectory = args[0];
        List<Callable<Boolean>> cloneTasks = new ArrayList<>();

        // TODO: get rid of temporary list
        for (String repoName : List.of(Main.morphia, Main.astminer)) {
            Callable<Boolean> cloneTask = () -> {
                try {
                    Main.cloneRepository(repoName, buildRootDirectory);
                } catch (Exception exception) {
                    System.out.println("Unable to create repository");
                    return false;
                }
                return true;
            };
            cloneTasks.add(cloneTask);
        }

        ExecutorService executor = Executors.newFixedThreadPool(cloneTasks.size());
        CompletionService<Boolean> completionService = new ExecutorCompletionService<>(executor);

        for (var cloneTask : cloneTasks) {
            completionService.submit(cloneTask);
        }

        for (int i = 0; i < 2; ++i) {
            try {
                var f = completionService.take();
                // very ugly code, but...
                if (!f.isDone()) {
                    try {
                        if (!f.get()) {
                            System.out.println("couldn't clone repository");
                        }
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Internal problem in completion service has occurred");
                // probably for production solution instant return wouldn't be the best idea
                return;
            }
        }

        ProjectBuildEnvironment astminerBuild = new ProjectBuildEnvironment(
                Main.astminer,
                Path.of(buildRootDirectory, Main.astminer).toString(),
                100
        );

        // '../' because for each version we create a new folder
        astminerBuild.buildAll("../gradlew");

        astminerBuild.printBuildStatistics();

        executor.shutdown();
    }

    public static void cloneRepository(String repositoryName, String directory)  throws IOException, InterruptedException {
        // TODO: get rid of magic constant 100
        String repoUrl = repoNameToRepoUrl.get(repositoryName);
        Main.executeCommands(
                "repo_clone",
                directory,
                String.format("git clone --depth=%d \"%s\" \"%s\"", 100, repoUrl, repositoryName)
        );
    }

    public static Process executeCommands(String name, String directory, String ... commands) throws IOException, InterruptedException {

        File tempScript = createTempScript(name, commands);
        File directoryFile = new File(directory);

        try {
            ProcessBuilder pb = new ProcessBuilder("bash", tempScript.toString());
            pb.directory(directoryFile);
            Process process = pb.start();
            process.waitFor();
            return process;
        } finally {
            // for temporary solution assert suffices
            assert tempScript.delete();
        }
    }

    public static File createTempScript(String name, String ... commands) throws IOException {
        File tempScript = File.createTempFile(name, null);

        Writer streamWriter = new OutputStreamWriter(new FileOutputStream(tempScript));
        PrintWriter printWriter = new PrintWriter(streamWriter);

        printWriter.println("#!/bin/bash");
        for (String command: commands) {
            printWriter.println(command);
        }

        printWriter.close();

        return tempScript;
    }

}