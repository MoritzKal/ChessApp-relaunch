"""Pydantic schemas for the self-play runner service."""
from __future__ import annotations

from pydantic import BaseModel, Field
from typing import Optional


class StartRequest(BaseModel):
    """Request payload to start a self-play run."""

    modelId: str
    baselineId: str
    games: int = Field(gt=0)
    concurrency: int = Field(gt=0)
    seed: int = 0


class RunStatus(BaseModel):
    """Status information about a running/completed self-play run."""

    runId: str
    status: str
    progress: dict[str, int]
    metrics: dict[str, Optional[float]]
    reportUri: Optional[str] = None


class MetricPayload(BaseModel):
    """Internal metric payload for tracking move statistics and errors."""

    runId: str
    result: Optional[str] = None
    error_type: Optional[str] = None
    move_time: Optional[float] = None
