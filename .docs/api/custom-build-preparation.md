## How can I build a custom build?

In short, (A)SWM requires the necessary spigot files in your local Maven repository. The easiest way to do this is to use Spigot's build tools [[Link](https://www.spigotmc.org/wiki/buildtools/)]. You can find all the information you need in the Wiki.

Summary from the Wiki:
- Download and install Git [[Link](https://git-scm.com/downloads)]
- Download and install Java 8 (AdoptOpenJDK works) [[Link](https://adoptopenjdk.net/?variant=openjdk8&jvmVariant=hotspot)]
- Download the BuildTools.jar [[Link](https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar)]
- Open a terminal or shell (the GIT shell also works) and navigate to the file BuildTools.jar
- Build the following glasses, they will automatically be added to your local Maven repo
  - `java -jar BuildTools.jar --rev 1.16.3`
  - `java -jar BuildTools.jar --rev 1.16.2`
  - `java -jar BuildTools.jar --rev 1.16.1`
- Make changes to the (A)SWM and compile it into Maven using `package'.
