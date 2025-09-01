PY ?= python3
DATASET ?= data/m2.parquet
EPOCHS ?= 8
BATCH ?= 512
LR ?= 3e-4
OUT ?= ml/training
RUN_NAME ?= baseline_tiny_policy
EXPERIMENT ?= training_baseline_cli

.PHONY: reqs install train train.cpu clean

reqs:
	$(PY) -m pip install -r training/requirements.txt

install: reqs

train:
	cd .. && $(PY) ml/training/train.py --data $(DATASET) --out-dir $(OUT) --epochs $(EPOCHS) --batch-size $(BATCH) --lr $(LR) --run-name $(RUN_NAME) --experiment $(EXPERIMENT)

train.cpu:
	cd .. && $(PY) ml/training/train.py --cpu --data $(DATASET) --out-dir $(OUT) --epochs $(EPOCHS) --batch-size $(BATCH) --lr $(LR) --run-name $(RUN_NAME) --experiment $(EXPERIMENT)

clean:
	rm -rf training/artifacts training/figures training/metrics.json training/params.json
