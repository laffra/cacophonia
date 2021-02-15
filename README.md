# Project Cacophonia

This project visualizes Eclipse to better understand how it works internally.

[![IMAGE ALT TEXT](http://img.youtube.com/vi/-lYNQ7SXGKY/0.jpg)](http://www.youtube.com/watch?v=-lYNQ7SXGKY "Project Cacophonia")

View the [Youtube video](http://www.youtube.com/watch?v=-lYNQ7SXGKY) explaining the motivation, implementation, and use of Cacophonia.

## Setup

 - Install and Launch Eclipse for Committers.
 - From the `Project Explorer`, Import a project from git.
 - Chose the URL  https://github.com/laffra/cacophonia.git
 - Switch back to the `Project Explorer` and open properties for the file `build_agent.sh`. Make it executable.
 - Run `Project > Clean...`
 
By now, you should see something looking like this:

![Cacophonia UI](/images/project-clean.png)

## Launch

 - Select the file `Cacophonia.launch` and run it as Eclipse launch named `Cacophonia`
 
   ![Cacophonia UI](/images/cacophonia-launch.png)
 
 - This will launch the Cacophonia UI:
 
   ![Cacophonia UI](/images/cacophonia-ui.png)
 
## Enabling Sound

Inside the Cacophonia UI, you can enable sounds when certain plugins interact:

 - Click the `mute` checkbox in the UI to toggle sound.
 - Switch sound themes by choosing a different theme in the dropdown showing `Cacophonia`.
 - Choose your own instruments by:
   - Click or drag one of the plugins you are interested in.
   - A red X will show it is now fixed in place.
   - Choose an instrument (e.g., `xylophone`) from the dropdown:

     ![Cacophonia UI](/images/core-job-organ.png)
 
   - If you hear nothing, toggle the mute button.
   
## The Cacophonia Implementation

Cacophonia exists of three separate components:

![Cacophonia UI](/images/architecture.png)

### The Agent

This is a Java agent, see [Agent.java](/src/cacophonia/Agent.java), which is passed to the JVM at startup.
The agent is enabled in the launch configuration. The `agent_build.sh` build script creates the jar and copies it to
your home directory. That jar is then passed to the JVM. See the launch configuration:

![Cacophonia UI](/images/eclipse-launch-configuration.png)

A byte array is passed to the Agent for each class being loaded during Eclipse's execution. That byte array is given to 
Javassist and each method found in every class is then instrumented to call the Cacophonia runtime. The result is that
when Eclipse launches, every time a method is entered and left, Cacophonia's runtime is notified. 
In the Eclipse console you can see what plugins and classes the agent sees coming by:

![Cacophonia UI](/images/eclipse-console.png)

Look for the `System.out.println` calls in [Agent.java](/src/cacophonia/Agent.java).

### The Runtime

The runtime, see [Cacophonia.java](/src/cacophonia/Cacophonia.java), is invoked by Eclipse at runtime.

After the code is loaded by the agent, the Cacophonia runtime will handle method enter and leave events to 
find out which plugin is calling which other plugin. This is done by using the classLoader for each object.
Namely, each eclipse plugin has its own unique classloader, which helps us locate the owning plugin for a 
given method call very easily. The only complication to worry about are multiple threads, so we create a 
callstack per thread and handle concurrent access.

Once we determined a call is being made from one plugin to another, we send an event to the remote UI, which
runs in another process. The UI is actually launched by the agent.

### The UI

Finally, the UI, see [UI.java](/src/cacophonia/UI.java), is running in a separate process.

The UI receives the event and draws a line between the two plugins in its UI. Timer threads are used to detect 
if a call took place between two plugins and to decay the line over time. The UI itself is a simple Java AWT
implementation using double-buffered drawing into a canvas.

The UI also has support for generating sounds for each plugin using Java's midi API.


## Why Do We Care?

The whole goal of Cacophonia and its UI is to show you what is happening inside Eclipse "under the hood". 
To paraphrase Richard Feynman, to most effectively use a tool, it is best to have a good understanding of how it works.

![Cacophonia UI](/images/feynman.png)

By observing the visualization, you may learn quite a few things about Eclipse. Examples:

 - The Eclipse splash screen could have a better progress bar. It does nothing for a while, then quickly jumps to 80%, and then just hangs there. A lot of plugins are still being loaded while no progress is shown in the splash screen. I care about this part quite a bit, as I used to spend a lot of time on the splash screen when I worked on Eclipse performance, a long time ago.
 - It takes 147 plugins to launch Eclipse and see the IDE come alive. For reference, in 2004, it took 26 plugins. 
 - It takes about 20s to launch with all the instrumentation happening, versus 10s normal launch time. So, startup is roughly 2X slower with Cacophonia. Interestingly enough, during normal usage of Eclipse, you cannot really tell the visualization is happening. Just when new features are loaded, you can tell a lot of extra work is being done. 
 - After Eclipse opens, 28 more plugins get loaded to a total of 175 plugins. By then, we loaded more than 7,000 Eclipse classes (JRE classes are not counted)
 - Switching to another perspective (say from Default to Plugin-in Development) adds only a handful of extra plugins.
 - With each "new" activity or task you will notice more plugins will need to be added. This shows the well-designed internal architecture of the Eclipse platform. The IDE is really one big monolith, but feature are only loaded and activated when needed. Plugins are decoupled from each other using the OSGI bundle framework. In that sense, Eclipse really acts like a micro-services architecture.
 - When you create a project and edit a Java file, you will notice Eclipse has a "heartbeat" to draw the insertion cursor. If you play around, you will see different heart beats. Different input fields involve different plugins and generate different patterns. 
 - When you open the Search dialog, you will notice that Eclipse is polling more aggressively now. A similar thing happens during debug. If you bring up the Activity Monitor, you will notice that Eclipse is actually spinning the CPU at around 10% of a core. 
 - Mylyn is involved in many things. It almost looks like a spying tool, but I am sure it is not.
 - If you used the Eclipse Installer, like I did, you will notice a lot of "oomph" plugins. Not sure what they all do... 
 - Set a breakpoint in your Java code and step into the code. Notice the UI and see how Eclipse supports debugging.
 - When you have a dialog open, such as the search dialog, click on the `?` icon and see what plugins are loaded. You will see Eclipse will run a webserver.

## Investigating Plugins In More Detail
 
Aside from the explorations shown above, after running Eclipse for a while, you may notice a certain Eclipse job running every 5 seconds. 
You can more easily notice the occurence of this event as follows:
 - In the Cacophonia UI, click on `manual`.
 - Select the "core jobs" plugin. It will turn bright red.
 - Look in the top of the UI and assign an instrument to the plugin, for instance "Marimba".
 
This will look like the following:

![Cacophonia UI](/images/core-jobs-marimba.png)

The plugin we selected, `core jobs`, is shown bright red. All the plugins that were invoked during the job's execution are light red. The 
lines between the plugins indicate Java calls made from one plugin to another. One of them that stands out quite a bit is 
the "mylyn monitor ui" plugin.

Let us investigate what the job is doing. For that, we will revisit the Cacophonia runtime in [Cacophonia.java](/src/cacophonia/Cacophonia.java).
In that file, locate the `Method.enter` method:

```java
public void enter() {
    callCount++;
    if (Cacophonia.debug) System.out.println(String.format("> %d %s %s", callCount, plugin, name));
    if (!lastPlugin.equals(plugin)) {
        Stack<String> stack = pluginStack.get();
        stack.push(plugin);
        remoteUI.sendEvent(String.format("%s %s", lastPlugin, plugin));
    }
    lastPlugin = plugin;
}
```

At the end of this method, we add an extra print statement:

```java
    if (plugin.startsWith("org.eclipse.core.jobs")) {
        System.out.println(String.format("# %d %s", callCount, name));
    }
```

When you save the file, you should see the agent build script run and update the agent jar with the new runtime.
Relaunch the Eclipse launch configuration now, and you should see something like this:

![Cacophonia UI](/images/trace-core-jobs.png)

This does show us all the methods invoked inside the `org.eclipse.core.jobs` plugin, but it does not tell us 
much yet what jobs are actually being run. We can update our runtime to pass the object itself and then print
out more details about the method being called. However, we don't know what to print yet. So, let us look at 
the plugin itself first. For that, we will import the plugin into our Eclipse workspace.

![Cacophonia UI](/images/import-plugin.png)

Eclipse jobs are essentially implemented as Java threads. This is explained in [this FAQ](https://wiki.eclipse.org/FAQ_Does_the_platform_have_support_for_concurrency%3F). 
So, to find out interesting things about running jobs, we will search for anything implementing a Java
thread. Such implementation will always override `void run()`, so we search for that.

![Cacophonia UI](/images/search.png)

The occurrence in `Worker.java` looks very promising as it appears to activate a job and then run it:

```java
    setName(getJobName());
    result = currentJob.run(monitor);
```

We change this code into:

```java
    setName(getJobName());
    System.out.println("Run job: " + currentJob.getClass().getName() + " '" + currentJob.getName() + "'");
    result = currentJob.run(monitor);
```

Notice that we are actually changing Eclipse's implementation this time. The nice thing about Eclipse is that
it is very easy to self-host, i.e., develop Eclipse using Eclipse. That is what we do now.

Save your changes and launch the Eclipse launch configuration again and you should see something like this happen:

![Cacophonia UI](/images/trace-jobs-worker.png)

Notice how all the print statements from our runtime changes still show up. If we remove those, things get less noisy:

![Cacophonia UI](/images/trace-jobs-worker-less-noise.png)

As you can see, the jobs that run at 5 second intervals appear to belong to the `org.eclipse.mylyn` plugins. A quick
Google search for Mylyn teaches us that it has been a part of Eclipse for a long time and its goal is to 
provide Eclipse with a task-focused interface to reduce information overload and makes multitasking easy. For that it
needs to keep track on how you use the Eclipse UI and what kind of task or activity is currently active. 

## Conclusions

In this project we show how easy it is to instrument Eclipse, visualize its execution, and quickly find out a lot
of things about a system consisting out of hundreds of plugins and thousands of classes. The total source size of
the Cacophonia project is around just 800 lines of Java. So, this is a tiny amount to get so much value.

Visualization is an effective teaching tool. The amount of information we glean by just watching and listening, 
is something that would have taken dozens or hundreds of hours of us setting breakpoints, reading source code, and
inserting print statements in the Eclipse source code. The biggest problem is that we would not know even where to
start our investigations. 

Visualization of complex systems increases our understanding of how the system works and helps us discover 
inefficiencies, anomolies, and problems more easily. 

Please clone the project, try it out, and let me know what you learned and what visualizations or fun sounds you came up with!
