#!/usr/bin/env bash

#
# Copyright (c) 2022.
#
# Author (Fork): Pedro Aguiar
# Original author: github.com/Grinderwolf/Slime-World-Manager
#
# Force, Inc (github.com/rede-force)
#

set -e

echo "Ensuring that pom  matches $TRAVIS_TAG"
mvn org.codehaus.mojo:versions-maven-plugin:2.5:set -DnewVersion=$TRAVIS_TAG

echo "Uploading to oss repo and GitHub"
mvn deploy --settings .travis/settings.xml -DskipTests=true --batch-mode --update-snapshots -Prelease