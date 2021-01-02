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
 
 ## Enabling Sound
 
 - Click the "mute" checkbox in the UI to toggle sound
 - Switch sound themes by choosing a different theme in the dropdown showing "Suspense"
 - Choose your own instruments by checking "manual"
   - All instruments will be muted
   - Select a given instrument and then choose an instrument from the dropdown.
   

 ![Cacophonia UI](/images/ui-swt-xylophone.png)
