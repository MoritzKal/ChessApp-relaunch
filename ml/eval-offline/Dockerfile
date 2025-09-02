FROM python:3.11-slim

WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1

RUN pip install --no-cache-dir uv
COPY pyproject.toml .
COPY app ./app
RUN uv pip install --system -e .
COPY eval.py ./eval.py

RUN useradd -m appuser
USER appuser

ENV PORT=8012
EXPOSE 8012
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8012"]
