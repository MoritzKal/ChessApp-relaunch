FROM python:3.11-slim

WORKDIR /app
COPY requirements.txt .
# Install runtime deps used by healthchecks and tools
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/* \
    && pip install --no-cache-dir -r requirements.txt

COPY *.py .

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8088"]
