FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app
COPY ml/requirements.txt requirements.txt
RUN python3 -m pip install --no-cache-dir -r requirements.txt

# Include full ml tree (app, service, tools, tests)
COPY ml /app/ml

# Ensure Python can import from /app/ml while working from /app
ENV PYTHONPATH=/app/ml
WORKDIR /app
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
