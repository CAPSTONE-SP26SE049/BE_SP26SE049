---
title: SpeakVN ASR Server
emoji: 🎙️
colorFrom: blue
colorTo: indigo
sdk: docker
app_port: 8000
pinned: false
---

# SpeakVN ASR (Speech-to-Text) Server

FastAPI server hosting NVIDIA NeMo Parakeet CTC 0.6B Vietnamese model for speech recognition and pronunciation assessment.

## Deployment on Hugging Face Spaces

This repository is pre-configured to run as a Docker Space on Hugging Face.

### Files Included:
- `server.py`: FastAPI application loading and serving the ASR model.
- `Dockerfile`: Production Docker build running on CPU.
- `README.md`: This file containing configuration metadata.
