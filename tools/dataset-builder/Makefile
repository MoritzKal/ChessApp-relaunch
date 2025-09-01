OUT ?= out

.PHONY: install test pgn build all

install: ; python -m pip install -r requirements.txt

test: ; pytest -q

pgn: ; python pgn_to_parquet.py --in $(IN) --out $(OUT)

build: ; @echo "TODO: build dataset from Parquet files"

all: ; @echo "TODO: run full dataset build pipeline"
