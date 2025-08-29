FROM python:3.11-slim

WORKDIR /app
COPY requirements.txt .
# Install minimal debugging/health tools and keep image slim
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl wget grep \
    && rm -rf /var/lib/apt/lists/* \
    && pip install --no-cache-dir -r requirements.txt
COPY . .
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001"]
