#!/bin/bash

echo "Starting Ollama server..."
ollama serve &
SERVE_PID=$!

echo "Waiting for Ollama server to be active..."
while ! ollama list | grep -q 'NAME'; do
  sleep 1
done

echo "Pulling ${AI_MODEL:-gemma3:1b} model..."
ollama pull ${AI_MODEL:-gemma3:1b}

wait $SERVE_PID 