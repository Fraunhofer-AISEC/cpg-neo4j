# Neo4J visualisation tool for the Code Property Graph 

A simple tool to export a *code property graph* to a neo4j database.

## Requirements

The application requires Java 11 or higher.

## Build

Build using Gradle

```
./gradlew installDist
```

## Usage

```
./build/install/cpg-vis-neo4j/bin/cpg-vis-neo4j <pathToSourcecode> [<pathToSourcecode> ...]
```
You can provide a list of paths of arbitrary length that can contain both file paths and directory paths.
