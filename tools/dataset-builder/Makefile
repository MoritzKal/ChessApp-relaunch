ROOT := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))
OUT ?= out
GAMES ?= $(OUT)/parquet/games.parquet
POS ?= $(OUT)/parquet/positions.parquet
FILTERS ?=

.PHONY: install test pgn build all

install:
	python -m pip install -r $(ROOT)/requirements.txt

test:
	pytest -q $(ROOT)/tests

pgn:
	python $(ROOT)/pgn_to_parquet.py --in $(IN) --out $(OUT)

build:
	python $(ROOT)/build_dataset.py --games $(GAMES) --positions $(POS) --name $(NAME) --out $(OUT) $(FILTERS)

all: pgn build
