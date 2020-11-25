mkdir -p .buildtools
wget -O .buildtools/BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
cd .buildtools
java -jar BuildTools.jar --rev 1.16.4