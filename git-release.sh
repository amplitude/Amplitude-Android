#!/bin/bash
# This script automates the process to release Amplitude Android SDK.
#
# EXAMPLE:
# ./release.sh [X.X.X]
# Note: X.X.X - release version (optional)
#
# If no version is given the next release version used will be the one that appears
# on gradle.properties (VERSION_NAME).

#RED='\033[0;31m'

# Get the release version from arg
releaseVersion=$1

if [[ ! -z $(git status -s) ]]; then
    printf "${RED}You have unstaged/untracked changes${NC}\n"
    exit
fi
