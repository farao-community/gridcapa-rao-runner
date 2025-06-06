#!/bin/bash

# Define function to unstash changes if needed
unstash() {
    if [ $STASHED_CHANGES ]; then
        git stash pop
    fi
}

# Step 1: Ask the user to enter tag name and to indicate if the version should be tagged as latest
read -p "Enter the tag name to checkout: " TAG_NAME
read -p "Tag the image as latest? (y/n): " TAG_LATEST

# Save the original branch to return to it later
ORIGINAL_BRANCH=$(git branch --show-current)

# Fetch git tags before checkout
git fetch --tags

# Disable detached head advice
git config advice.detachedHead false

# If changes exist in local branch, stash them
if [ $(git status -s | wc -l) -gt 0 ]; then
    git stash
    STASHED_CHANGES=true
fi

# Step 2: Checkout the tag entered by the user
git checkout $TAG_NAME
if [ $? -ne 0 ]; then
    echo "Error checking out tag $TAG_NAME"
    git config advice.detachedHead true  # Re-enable the advice
    unstash # Unstash changes if needed
    exit 1
fi

# Step 3: Clean install to construct the .jar
mvn clean install

# Step 4: Build the Docker image
IMAGE_NAME="farao/gridcapa-rao-runner-with-xpress:$TAG_NAME"
docker build -f Dockerfile_with_xpress -t $IMAGE_NAME .
if [ $? -ne 0 ]; then
    echo "Error building the Docker image"
    git config advice.detachedHead true  # Re-enable the advice
    git checkout $ORIGINAL_BRANCH
    unstash # Unstash changes if needed
    exit 1
fi

# Step 5: Check if the Docker image exists in the repository
docker manifest inspect $IMAGE_NAME
if [ $? -eq 0 ]; then
    echo "The Docker image $IMAGE_NAME already exists."
    read -p "Do you want to overwrite it? (y/n): " CONFIRM
    if [ "$CONFIRM" != "y" ]; then
        echo "Aborting the push."
        git config advice.detachedHead true  # Re-enable the advice
        git checkout $ORIGINAL_BRANCH
        unstash # Unstash changes if needed
        exit 1
    fi
else
    echo "The Docker image $IMAGE_NAME does not exist on repository. Proceeding with the push."
fi

# Step 6: Push the Docker image
docker push $IMAGE_NAME
if [ $? -ne 0 ]; then
    echo "Error pushing the Docker image"
    git config advice.detachedHead true  # Re-enable the advice
    git checkout $ORIGINAL_BRANCH
    unstash # Unstash changes if needed
    exit 1
fi

# Step 7: Tag and push the Docker image as latest
if [ "$TAG_LATEST" == "y" ]; then
    IMAGE_NAME_LATEST="farao/gridcapa-rao-runner-with-xpress:latest"
    docker tag $IMAGE_NAME $IMAGE_NAME_LATEST
    if [ $? -ne 0 ]; then
        echo "Error tagging the Docker image as latest"
        git config advice.detachedHead true  # Re-enable the advice
        git checkout $ORIGINAL_BRANCH
        unstash # Unstash changes if needed
        exit 1
    fi

    docker push $IMAGE_NAME_LATEST
    if [ $? -ne 0 ]; then
        echo "Error pushing the latest Docker image"
        git config advice.detachedHead true  # Re-enable the advice
        git checkout $ORIGINAL_BRANCH
        unstash # Unstash changes if needed
        exit 1
    fi
fi


# Step 8: Checkout the original branch
git checkout $ORIGINAL_BRANCH
if [ $? -ne 0 ]; then
    echo "Error checking out the original branch $ORIGINAL_BRANCH"
    git config advice.detachedHead true  # Re-enable the advice
    # No unstash here as the original branch could not be checked-out: the changes remain available in stash
    exit 1
fi

# Re-enable detached head advice
git config advice.detachedHead true

# Unstash changes if needed
unstash

echo "Script executed successfully"
