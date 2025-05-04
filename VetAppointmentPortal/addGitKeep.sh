#!/bin/bash

# Script to add .gitkeep files to all empty directories in a project

# Print usage information
echo "Adding .gitkeep files to empty directories..."

# Find all empty directories in the current directory and subdirectories
# -type d: find directories
# -empty: find empty directories
# -not -path "*/\.*": exclude hidden directories (like .git)
find . -type d -empty -not -path "*/\.*" | while read -r dir; do
    echo "Adding .gitkeep to empty directory: $dir"
    touch "$dir/.gitkeep"
done

echo "Done! Empty directories will now be tracked by Git."
echo "Don't forget to:"
echo "1. git add ."
echo "2. git commit -m \"Add .gitkeep files to track empty directories\""
echo "3. git push origin <your-branch>"
