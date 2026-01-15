# Makefile for AI Backend Service

.PHONY: help install run test clean lint format

help:
	@echo "Available commands:"
	@echo "  make install    - Install dependencies"
	@echo "  make run        - Run the application"
	@echo "  make test       - Run tests"
	@echo "  make clean      - Clean cache and build files"
	@echo "  make lint       - Run linters"
	@echo "  make format     - Format code"
	@echo "  make setup      - Initial setup"

install:
	pip install -r requirements.txt

run:
	uvicorn src.presentation.main:app --reload --host 0.0.0.0 --port 8000

test:
	pytest tests/ -v

test-cov:
	pytest tests/ --cov=src --cov-report=html --cov-report=term

clean:
	find . -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
	find . -type f -name "*.pyc" -delete
	find . -type f -name "*.pyo" -delete
	find . -type d -name "*.egg-info" -exec rm -rf {} + 2>/dev/null || true
	rm -rf .pytest_cache .coverage htmlcov/ dist/ build/
	rm -f *.db

lint:
	@echo "Running flake8..."
	flake8 src/ --max-line-length=120 || true
	@echo "Running mypy..."
	mypy src/ --ignore-missing-imports || true

format:
	black src/ tests/ --line-length=120
	isort src/ tests/

setup:
	python -m venv venv
	@echo "Virtual environment created. Activate it with:"
	@echo "  source venv/bin/activate  (macOS/Linux)"
	@echo "  venv\\Scripts\\activate     (Windows)"

dev-install:
	pip install -r requirements.txt
	pip install black isort flake8 mypy pytest-cov

docker-build:
	docker build -t ai-backend-service .

docker-run:
	docker run -p 8000:8000 ai-backend-service

check:
	python test_setup.py

