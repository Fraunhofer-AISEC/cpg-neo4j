# Neo4J visualisation tool for the Code Property Graph 

A simple tool to export a *code property graph* to a neo4j database.

## Build

Build using Gradle

```
./gradlew nativeImage
```

## Usage

Start neo4j using `docker run -d --env NEO4J_AUTH=neo4j/password -p7474:7474 -p7687:7687 neo4j:3.5`

```
./build/graal/cpg-vis-neo4j <pathToSourcecode> [<pathToSourcecode> ...]
```
You can provide a list of paths of arbitrary length that can contain both file paths and directory paths.
