FROM python:3.11-slim
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1
WORKDIR /app
# Install runtime deps
COPY ml/serve/requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir -r /app/requirements.txt
# Copy app
COPY ml /app/ml
EXPOSE 8001
CMD ["uvicorn", "ml.serve.app:app", "--host", "0.0.0.0", "--port", "8001", "--no-server-header"]
