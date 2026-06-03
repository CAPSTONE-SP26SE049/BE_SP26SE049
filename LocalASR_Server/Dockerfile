FROM python:3.10-slim

# Install system dependencies needed for NeMo & libsndfile
RUN apt-get update && apt-get install -y \
    libsndfile1 \
    ffmpeg \
    git \
    g++ \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Upgrade pip
RUN pip install --no-cache-dir --upgrade pip

# Install PyTorch CPU version (lighter for Docker deployment without GPU)
RUN pip install --no-cache-dir torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cpu

# Install FastAPI and Uvicorn
RUN pip install --no-cache-dir fastapi uvicorn python-multipart

# Install NeMo ASR
RUN pip install --no-cache-dir nemo_toolkit[asr]

# Copy the server script
COPY server.py .

# Expose port (FastAPI default, overridden by PORT env var)
EXPOSE 8000

# Set environment variable to store Hugging Face cache in a writable directory
ENV HF_HOME=/app/cache

# Run the FastAPI server
CMD ["python", "server.py"]
