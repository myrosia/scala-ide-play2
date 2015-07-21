#!/bin/bash
mvn -Peclipse-luna -Pscala-2.11.x -Drepo.scala-ide="http://download.scala-ide.org/pr-builds/scala-ide/pr-978/2.11.x/site" clean verify
