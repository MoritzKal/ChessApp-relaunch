# Datenmodelle (UI-relevant, komprimiert)

**TrainingRun** `{ runId, status, epoch?, loss?, val_acc_top1?, startedAt, finishedAt? }`

**Dataset** `{ id, name, version, size_rows, split:{train, val, test}, filter:{...} }`

**Model** `{ id, name, version, stage: "staging"|"prod", metrics:{best_val_acc?, ...} }`

**PredictRequest** `{ fen:string, history?:string[], temperature?:number, topk?:number }` → **PredictResponse** `{ move:string, policy:Array<{uci:string, p:number}> }`
