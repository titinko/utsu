# UTSU - A cross-platform vocal synthesis frontend

Compatible (in theory) with all voicebanks, files, and engines created with or for the UTAU software--however, the two programs have no code in common and both user features and backend processing can and will be different.

## Installation instructions

### The easy way--installers!

For Mac:
Download the installer [here](https://drive.google.com/open?id=1SHrB--WL492QEcgrQR8But6Jj66SCnrC).

For Windows:
Coming on 1/15/2018!

For Linux:
TBA

### The hard way--a JAR file

For Mac, Windows, Linux:
First, you'll need to make sure you computer runs Java.  You can download the latest Java engine [here](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

![Here's what the download page should look like.](images/java_screenshot.png)
You'll need Java 8, not Java 7 or Java 9, and you'll want a JRE, not a JDK.

Next, download the Utsu executable [here](https://drive.google.com/open?id=14p_ZhsqQsPaiw2QmlwfzTH-RmCvfpCm3). 

Unzip the downloaded file and you should see a JAR file and an assets folder.  Confirm that the assets folder and the Utsu file are in the same parent folder, and double-click the JAR file to start Utsu.  If the program doesn't run properly the first time, try closing and double-clicking the JAR file again.  If it still doesn't work, you can try running it from the terminal:
> java -jar /path/to/jarfile/utsu-0.1.jar

Downloading the JAR file is much more space-efficient than using the installer, if you're worried about that kind of thing.

## Building from source

Please install Maven v. 4.0.0 or higher.
Please ensure your Java version is 8 or higher.

Navigate to the same directory as the pom.xml file and type:
> mvn clean verify

in the command line.  You can look up various other Maven commands to test, deploy, etc.