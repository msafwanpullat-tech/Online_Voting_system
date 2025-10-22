#!/bin/bash

echo "Starting Voting System Server..."
echo ""
echo "Make sure Java is installed and in your PATH"
echo ""

# Compile the Java file
javac VotingSystemServer.java
if [ $? -ne 0 ]; then
    echo "Compilation failed! Please check Java installation."
    exit 1
fi

echo "Server compiled successfully!"
echo "Starting server on http://localhost:8080"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

# Run the server
java VotingSystemServer

