# cacophonia
Run Eclipse with bytecode instrumentation to learn about its execution

## Setup

 - Launch Eclipse for Committers.
 - From the Project Explore, Import a project from git.
 - Chose the URL "https://github.com/laffra/cacophonia.git"
 - Import all Eclipse projects
 - Switch to the project and open properties for the file "build_agent.sh". Make it executable.
 - Run Project > Clean
 
By now, you should see something looking like this:

 ![Cacophonia UI](/images/project-clean.png)

## Launch

 - Select "Cacophonia.launch" and run it as Eclipse launch named "Cacophonia"
 - This will launch the Cacophonia UI
 
 ![Cacophonia UI](/images/ui-launch.png)
