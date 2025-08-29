FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

# Ensure stable imports: top-level /app on sys.path
ENV PYTHONPATH=/app:${PYTHONPATH}
ENV PROMETHEUS_MULTIPROC_DIR=/tmp/chs_prom
RUN mkdir -p /tmp/chs_prom

COPY ml/requirements.txt /app/ml/requirements.txt
RUN pip install --no-cache-dir -r /app/ml/requirements.txt

COPY ml/app /app/app
# Include tests for in-container pytest runs
COPY ml/tests /app/ml/tests
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
