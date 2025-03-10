.PHONY: all repl zrepl format-fix format-check lint test

all: format-fix lint test

repl:
	clojure -M:test:nrepl:refactor

zrepl:
	clojure -M:test:zrepl

format-fix:
	clojure -M:format fix

format-check:
	clojure -M:format check

lint:
	clojure -M:lint

test:
	clojure -M:test
