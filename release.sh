#!/bin/bash
# This script automates all the tasks needed to make a new Amplitude Android SDK release.
#
# Usage: ./release.sh [X.X.X] where X.X.X is the release version. This param is optional.
#
# If no version is given the next release version used will be the one that appears
# on gradle.properties (ARTIFACT_VERSION).

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
ORANGE='\033[0;33m'
NC='\033[0m'

if [ ! -f gradle.properties ]; then
    printf "${RED}gradle.properties was not found. Make sure you are running this script from its root folder${NC}\n"
    exit
fi
if [[ ! -z $(git status -s) ]]; then
    printf "${RED}You have unstaged/untracked changes${NC}\n"
    exit
fi

abort () {
    restoreFiles
    cleanUp
    quit
}

quit () {
    git checkout ${originalBranch}
    exit
}

cleanUp () {
    if [ -f gradle.properties.bak ]; then
        rm gradle.properties.bak
    fi
    if [ -f README.md.bak ]; then
        rm README.md.bak
    fi
    if [ -f changes.txt ]; then
        rm changes.txt
    fi
}

restoreFiles () {
    git checkout -- gradle.properties
    git checkout -- README.md
}

# find release version: if no args we grab gradle.properties without -SNAPSHOT
if [ -z "$1" ]
  then
    releaseVersion=$(head -n 1 gradle.properties | sed -e 's/ARTIFACT_VERSION=\(.*\)-SNAPSHOT/\1/')
else
    releaseVersion=$1
fi
echo $releaseVersion | grep -q "^[0-9]\+.[0-9]\+.[0-9]$"
if [ ! $? -eq 0 ] ;then
    printf "${RED}Wrong version format (X.X.X) for: $releaseVersion\n"
    printf "Check your gradle.properties file or the argument you passed.${NC}\n"
    exit
fi

# This is the current branch you're at.
originalBranch=$(git symbolic-ref HEAD | sed -e 's,.*/\(.*\),\1,')
releaseBranch=master
docBranch=gh-pages

# checkout release branch
printf "${YELLOW}Checking out $releaseBranch...${NC}\n"
git checkout ${releaseBranch}
git pull origin ${releaseBranch}

# change version on gradle.properties - Make sure there are no spaces. Expected format: VERSION_NAME=.*
sed -i.bak 's,^\(ARTIFACT_VERSION=\).*,\1'$releaseVersion',w changes.txt' gradle.properties
if [ ! -s changes.txt ]; then
    printf "\n${RED}Err... gradle.properties was not updated. The following command was used:\n"
    printf "sed -i.bak 's,^\(VERSION_NAME=\).*,\1'$releaseVersion',' gradle.properties${NC}\n\n"
    abort
fi
rm changes.txt

newDate=$(date "+%B %d\, %Y") # Need the slash before the comma so next command does not fail

echo "Date: ${newDate}"
sed -i.bak "s,^\(##### _\).*\(_ - \[v\).*\(](https://github.com/amplitude/Amplitude-Android/releases/tag/v\).*\()\),\1$newDate\2$releaseVersion\3$releaseVersion\4,w changes.txt" README.md
if [ ! -s changes.txt ]; then
   printf "\nErr... Fail to update README.md for release date.\n"
   abort
fi

rm changes.txt

echo "######### CHANGES BEGIN ############"
printf "\n"
git --no-pager diff
printf "\n\n\n"
echo "######### CHANGES END ############"

# remove backup files
cleanUp

# upload library to maven
printf "\n\n${YELLOW}Uploading archives...${NC}\n"
if ! ./gradlew uploadArchives ; then
    printf "${RED}Err.. Seems there was a problem runing ./gradlew uploadArchives\n${NC}"
    abort
fi

# commit new version
printf "\n\n${YELLOW}Pushing changes...${NC}\n"
git commit -am "New release: ${releaseVersion}"
git push origin $releaseBranch

# create new tag
newTag=v${releaseVersion}
printf "\n\n${YELLOW}Creating new tag $newTag...${NC}\n"
git tag ${newTag}
git push origin ${newTag}

# find next snapshot version by incrementing the release version
nextSnapshotVersion=$(echo $releaseVersion | awk -F. -v OFS=. 'NF==1{print ++$NF}; NF>1{if(length($NF+1)>length($NF))$(NF-1)++; $NF=sprintf("%0*d", length($NF), ($NF+1)%(10^length($NF))); print}')-SNAPSHOT

# update next snapshot version
printf "\n${YELLOW}Updating next snapshot version...${NC}\n"
sed -i.bak 's,^\(ARTIFACT_VERSION=\).*,\1'${nextSnapshotVersion}',' gradle.properties
git --no-pager diff
printf '\n\n\n'
git commit -am "Update master with next snasphot version $nextSnapshotVersion"
git push origin master

# remove backup files
cleanUp

# update documentation
printf "\n\n${YELLOW}Updating documentation...${NC}\n\n"
git checkout ${docBranch}
git pull origin ${docBranch}
cp -r build/docs/javadoc/* .
git add .
git commit -m "Update documentation for ${releaseVersion}"
git push origin gh-pages

printf "\n${GREEN}All done! ¯\_(ツ)_/¯ \n"
printf "Make sure you make a new release at https://github.com/amplitude/amplitude-android/releases/new\n"
printf "Also, do not forget to update our CHANGELOG (https://github.com/amplitude/amplitude-android/wiki/Changelog)\n"
printf "And finally, release the library from https://oss.sonatype.org/index.html\n\n${NC}"

quit
