#!/bin/bash
# This script automates the process to release Amplitude Android SDK.
#
# EXAMPLE:
# ./release.sh [X.X.X]
# Note: X.X.X - release version (optional)

#if [[ ! -z $(git status -s) ]]; then
#    printf "${RED}You have unstaged/untracked changes${NC}\n"
#    exit
#fi

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
    git checkout -- README.md
}

quit () {
    mv ~/.gradle/gradle.properties ~/.gradle/gradle.properties.bak
    git checkout $originalBranch
    exit
}

abort () {
    restoreFiles
    cleanUp
    quit
}

if [ -z "$1" ]; then
    echo "No version is specified as command argument. Exiting"
    quit
else
    releaseVersion=$1
fi

# Check if the release version is format (X.X.X)
echo $releaseVersion | grep -q "^[0-9]\+.[0-9]\+.[0-9]$"
if [ ! $? -eq 0 ]; then
    printf "Wrong version format (X.X.X) for: $releaseVersion\n"
    printf "Check your gradle.properties file or the argument you passed.\n"
    exit
fi

echo "release version is ${releaseVersion}"

# This is the current branch you're at.
originalBranch=$(git symbolic-ref HEAD | sed -e 's,.*/\(.*\),\1,')
releaseBranch=master
docBranch=gh-pages
#
## Checkout master branch for releasing.
##printf "Checking out $releaseBranch...${NC}\n"
##git checkout $releaseBranch
##git pull origin $releaseBranch

newDate=$(date "+%B %d\, %Y") # Need the slash before the comma so next command does not fail

#echo "Date: ${newDate}"
#sed -i.bak "s,^\(##### _\).*\(_ - \[v\).*\(](https://github.com/amplitude/Amplitude-Android/releases/tag/v\).*\()\),\1$newDate\2$releaseVersion\3$releaseVersion\4,w changes.txt" README.md
#if [ ! -s changes.txt ]; then
#    printf "\nErr... Fail to update README.md for release date.\n"
#    abort
#fi

rm changes.txt

printf "\n"
git --no-pager diff
printf "\n\n\n"

# remove backup files
cleanUp

## commit new version
#printf "\n\nPushing changes...\n"
#git commit -am "New release: ${releaseVersion}"
## push changes
#git push origin $releaseBranch

# create new tag
newTag=v${releaseVersion}
printf "\n\nCreating new tag $newTag...\n"
git tag ${newTag}
git push origin ${newTag}

# update documentation
printf "\n\nUpdating documentation...\n\n"
git checkout ${docBranch}
git pull origin ${docBranch}
cp -r build/docs/javadoc/* .
git add .
git commit -m "Update documentation for 2.23.2"
git push origin gh-pages

#
## upload library to maven??

printf "\nAll done!\n"
printf "Make sure you make a new release at https://github.com/amplitude/amplitude-android/releases/new\n"
printf "Also, do not forget to update our CHANGELOG (https://github.com/amplitude/amplitude-android/wiki/Changelog)\n"

quit
