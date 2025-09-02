"""Pydantic models for evaluation service."""
from pydantic import BaseModel, Field
from typing import Dict, Optional


class EvalStartRequest(BaseModel):
    model_id: str = Field(alias="modelId")
    dataset_id: Optional[str] = Field(default=None, alias="datasetId")
    metrics: Optional[list[str]] = None
    batch_size: int = Field(default=32, alias="batchSize")
    limit: int = 200
    seed: int = 0

    class Config:
        populate_by_name = True


class EvalStatus(BaseModel):
    eval_id: str = Field(alias="evalId")
    status: str
    metrics: Dict[str, float] | None = None
    report_uri: str | None = Field(default=None, alias="reportUri")

    class Config:
        populate_by_name = True
