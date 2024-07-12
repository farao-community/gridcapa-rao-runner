#!/bin/bash

# Step 1: Ask the user to enter two variables
echo "Enter the tag name to checkout: "
read TAG_NAME

echo "Enter the image version to build (format vX.XX.XX): "
read IMAGE_VERSION

# Save the original branch to return to it later
ORIGINAL_BRANCH=$(git branch --show-current)

# Disable detached head advice
git config advice.detachedHead false

# Step 2: Checkout the tag entered by the user
git checkout $TAG_NAME
if [ $? -ne 0 ]; then
    echo "Error checking out tag $TAG_NAME"
    git config advice.detachedHead true  # Re-enable the advice
    exit 1
fi

# Step 3: Modify the Dockerfile
DOCKERFILE="Dockerfile"
if [ -f "$DOCKERFILE" ]; then
    ORIGINAL_LINE=$(grep -E '^FROM farao/farao-computation-base:[0-9]+\.[0-9]+\.[0-9]+' $DOCKERFILE)
    if [ -n "$ORIGINAL_LINE" ]; then
        MODIFIED_LINE="FROM inca.rte-france.com/farao/farao-computation-base-with-xpress:1.8.0"
        sed -i "s|$ORIGINAL_LINE|$MODIFIED_LINE|" $DOCKERFILE
        if [ $? -ne 0 ]; then
            echo "Error modifying the Dockerfile"
            git config advice.detachedHead true  # Re-enable the advice
            git checkout $ORIGINAL_BRANCH
            exit 1
        else
            echo "Replaced line \"$ORIGINAL_LINE\" with \"$MODIFIED_LINE\" in $DOCKERFILE"
        fi
    else
        echo "No matching FROM line found in Dockerfile"
        git config advice.detachedHead true  # Re-enable the advice
        git checkout $ORIGINAL_BRANCH
        exit 1
    fi
else
    echo "Dockerfile not found"
    git config advice.detachedHead true  # Re-enable the advice
    git checkout $ORIGINAL_BRANCH
    exit 1
fi

# Step 4: Build the Docker image
IMAGE_NAME="farao/gridcapa-rao-runner-with-xpress:$IMAGE_VERSION"
docker build -t $IMAGE_NAME .
if [ $? -ne 0 ]; then
    echo "Error building the Docker image"
    # Rollback the Dockerfile
    sed -i "s|$MODIFIED_LINE|$ORIGINAL_LINE|" $DOCKERFILE
    git config advice.detachedHead true  # Re-enable the advice
    git checkout $ORIGINAL_BRANCH
    exit 1
fi

# Step 5: Push the Docker image
docker push $IMAGE_NAME
if [ $? -ne 0 ]; then
    echo "Error pushing the Docker image"
    # Rollback the Dockerfile
    sed -i "s|$MODIFIED_LINE|$ORIGINAL_LINE|" $DOCKERFILE
    git config advice.detachedHead true  # Re-enable the advice
    git checkout $ORIGINAL_BRANCH
    exit 1
fi

# Rollback the Dockerfile
sed -i "s|$MODIFIED_LINE|$ORIGINAL_LINE|" $DOCKERFILE
if [ $? -ne 0 ]; then
    echo "Error rolling back the Dockerfile"
    git config advice.detachedHead true  # Re-enable the advice
    git checkout $ORIGINAL_BRANCH
    exit 1
fi

# Checkout the original branch
git checkout $ORIGINAL_BRANCH
if [ $? -ne 0 ]; then
    echo "Error checking out the original branch $ORIGINAL_BRANCH"
    git config advice.detachedHead true  # Re-enable the advice
    exit 1
fi

# Re-enable detached head advice
git config advice.detachedHead true

echo "Script executed successfully"
