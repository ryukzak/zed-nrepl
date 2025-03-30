.PHONY: all repl zed-repl format-fix format-check lint test

all: format-fix lint test

repl:
	clojure -M:test:nrepl:refactor

zed-repl:
	clojure -M:test:zed-repl

format-fix:
	clojure -M:format fix

format-check:
	clojure -M:format check

lint:
	clojure -M:lint

test:
	clojure -M:test
