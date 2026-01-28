#!/bin/bash

# Start the FastAPI server
echo "🚀 Starting AI Backend Service..."
./venv/bin/uvicorn src.presentation.main:app --reload --host 0.0.0.0 --port 8000

