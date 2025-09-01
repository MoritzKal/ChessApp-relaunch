OUT ?= out

.PHONY: install test pgn build all

install: ; python -m pip install -r requirements.txt

test: ; pytest -q

pgn: ; python pgn_to_parquet.py --in $(IN) --out $(OUT)

build: ; \
python build_dataset.py --games $(GAMES) --positions $(POS) --name $(NAME) --out $(OUT) \
$(if $(MIN_ELO),--min-elo $(MIN_ELO)) \
$(if $(MAX_ELO),--max-elo $(MAX_ELO)) \
$(if $(SINCE),--since $(SINCE)) \
$(if $(TIME_CONTROL),--time-control $(TIME_CONTROL)) \
$(if $(ECO),--eco $(ECO)) \
$(if $(COLOR),--color $(COLOR)) \
$(if $(TRAIN),--train $(TRAIN)) \
$(if $(VAL),--val $(VAL)) \
$(if $(TEST),--test $(TEST)) \
$(if $(SEED),--seed $(SEED))

all: ; @echo "TODO: run full dataset build pipeline"
