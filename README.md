# Test prioritization test task

Unfortunately, I didn't implement build for morphia because I I didn't have enough time to figure out how to build it (although, it is possible that `mvn build` or something like that would work), however my solution is abstract enough to make it work (if right build command is known).

In order to run my solution, you need to have **Linux** (I myself tested it on **Ubuntu 20.04**), `git` (plus ssh key for github already set up).

You need to pass one argument to my program, it is directory where I will clone projects and make builds (it should be relative path from working directory).

I myself did it right in intelliJ. In order to do that, you need to "Edit Configurations", put there working directory that you want and argument that my program requires.