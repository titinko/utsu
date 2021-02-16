# UTSU - A cross-platform vocal synthesis frontend

Compatible (in theory) with all voicebanks, files, and engines created with or for the UTAU software--however, the two programs have no code in common and both user features and backend processing can and will be different.

Supported languages: English, Español, Français, 日本語, 中文, Italiano, Bahasa Indonesia, Português Brasileiro, русский, հայերեն

Discord: https://discord.gg/5nRxvd6nK7

## Installation instructions

### Windows

The Windows installer (64-bit MSI) can be found on the [releases page](https://github.com/titinko/utsu/releases).

### Mac

The Mac installer (64-bit DMG) can be found on the [releases page](https://github.com/titinko/utsu/releases).

### Linux

The Linux installer (64-bit DEB) can be found on the [releases page](https://github.com/titinko/utsu/releases).

For non-Debian users, a raw 64-bit Linux executable can be found on the [releases page](https://github.com/titinko/utsu/releases)
as a zip file.

## Building from source

### Java

For best results, use [Oracle Java 14](https://www.oracle.com/java/technologies/javase/jdk14-archive-downloads.html).
Open-jfx seems to be missing some of the packaging tools I use in my setup and Oracle Java 15 has a bug when creating
Windows installers. The methods for downloading and installing Java vary depending on your operating system, but at the
end you'll need the JAVA_HOME variable to be set on your machine for Utsu to build properly.

### Maven

Download [Apache Maven](https://maven.apache.org/download.cgi) and install it using the instructions
[here](https://maven.apache.org/install.html).

### Running Utsu

Popular IDEs such as Eclipse and IntelliJ will give you the option to create a new project by copying from a Git
repository. You can also use the git command-line tool or even a Git GUI (there's one for Windows [here](https://gitforwindows.org/))
to download the repository to a directory of your choice.

To build and run Utsu, navigate to the same directory as the pom.xml file and type:
> mvn javafx:run

See https://github.com/titinko/utsu/wiki/Compiling for platform-specific compilation examples.
