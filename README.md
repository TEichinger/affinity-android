Affinity - Share Movie Ratings in Physical Proximity
=====================================

Affinity is an Android mobile application. The application allows users to (a) create a profile of movie ratings and (b) share it with nearby other users through ad-hoc wireless connections.
Ratings from other user are used to derive recommendations on-device. 

The current version of the Affinity Android application is **1.2-alpha (9023000)**.

The Affinity mobile application leverages the [Google Nearby Connections API](https://developers.google.com/nearby/connections/overview) and the [Google Activity Recognition API](https://developers.google.com/location-context/activity-recognition). The Affinity Android Application stores ratings in a [Room](https://developer.android.com/jetpack/androidx/releases/room?gclid=CjwKCAjwsNiIBhBdEiwAJK4khixAOc4zMfBSjbOkoTUrVOl9cvAhZ3upBvJidU6_IYnWE6XXy3yZ2RoCw1MQAvD_BwE&gclsrc=aw.ds) database.

# 1. Build the App
-------------

The Affinity mobile application requires the following **build software**:

| Tool                                   | Version |
| -------------                         | ------------- |
| Gradle                                | 7.3.3  |
| JDK                                   | 11.0.12 |

Building makes use of the following **plugins**:

| Plugin                                | Version |
| -------------                         | ------------- |
| Android Gradle Plugin (AGP)            | 7.2.1  |
| Kotlin (KGP)                          | 1.5.21  |

Builds target the **Android SDK API Version 30**.


## 1.1 Clone the Repository

```
git clone https://github.com/TEichinger/affinity-android.git
cd affinity-android
```

## 1.2 Download Build Software  

### 1.2.1 Download Gradle v7.3.3 

Download [Gradle version 7.3.3](https://services.gradle.org/distributions) as a .zip file, unzip it.

```
curl -L -o ./gradle-7.3.3-all.zip https://services.gradle.org/distributions/gradle-7.3.3-all.zip
unzip ./gradle-7.3.3-all.zip -d ./
```

Check whether Gradle v7.3.3 is set up correctly:

```
./gradle-7.3.3/bin/gradle --version
>>> ------------------------------------------------------------
>>> Gradle 7.3.3
>>> ------------------------------------------------------------

>>> Build time:   2021-12-22 12:37:54 UTC
>>> Revision:     6f556c80f945dc54b50e0be633da6c62dbe8dc71

>>> Kotlin:       1.5.31
>>> Groovy:       3.0.9
>>> Ant:          Apache Ant(TM) version 1.10.11 compiled on July 10 2021
>>> JVM:          18.0.1.1 (Oracle Corporation 18.0.1.1+2-6)
>>> OS:           Mac OS X 12.6.5 x86_64
```

### 1.2.1 Download JDK v11.0.12 

Download [JDK version 11.0.12](https://www.oracle.com/de/java/technologies/javase/jdk11-archive-downloads.html) for your platform. You need to note down the path to its 'Contents/Home' folder, e.g. '/Library/Java/JavaVirtualMachines/jdk-11.0.12.jdk/Contents/Home'. In this README we refer to this path as <JAVA11PATH>.

Check whether JDK v11.0.12 is set up correctly:

```
<JAVA11PATH>/bin/java --version
>>> java 11.0.12 2021-07-20 LTS
>>> Java(TM) SE Runtime Environment 18.9 (build 11.0.12+8-LTS-237)
>>> Java HotSpot(TM) 64-Bit Server VM 18.9 (build 11.0.12+8-LTS-237, mixed mode)
```

### 1.3 Build the App

Create a gradle wrapper files 'gradlew' (for Linux/MacOS) and 'gradlew.bat' (for Windows).

```
./gradle-7.3.3/bin/gradle wrapper
```

Execute the gradle wrapper file to build the app. 
```
./gradlew clean build -Dorg.gradle.java.home="<JAVA11PATH>" 
```

You can find Android Package (.apk) files of the Affinity mobile app in the sub-directories of the './app/build/outputs/apk/debug/app-debug.apk' directory (e.g. './app/build/outputs/apk/'). These .apk files can be installed on an Android smartphone.


Contributors
------------

Special thanks to the numerous contributors who helped realize this project: R. Papke, L. Rebscher, HC Tran, M. Trzeciak, L.Liegener, S.Saini, D. Koljada, A. Thieme, S.Högl, F.Schneider, T.Ögel, TH Minh, P. Willmann, M. Digtiar, D. Romanchenko, S. Ugriumov. 
