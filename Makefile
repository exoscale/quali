.PHONY: *
.DEFAULT_GOAL:=help

# Internal
UNAME_S=$(shell uname -s)
ROOT_DIR=$(shell dirname $(realpath $(firstword $(MAKEFILE_LIST))))
VERSION=$(shell cat $(ROOT_DIR)/VERSION)
CLJ=clojure -J-Dclojure.main.report=stderr -J--enable-preview
RM=rm -f
CLEANFILES=node_modules package.json package-lock.json out target

##@ Standard build tasks

node_modules/ws:
	npm install ws

test-clj: ## Runs Clojure unit test
	$(CLJ) -M:test clj
.PHONY: test-clj

test-cljs: node_modules/ws ## Runs Clojure unit test
	$(CLJ) -M:test cljs
.PHONY: test-cljs

test: test-clj test-cljs ## Runs all unit tests
.PHONY: test

repl: ## Launch repl
	$(CLJ) -A:test
.PHONY: repl

clean: ## Remove transient files
	$(RM) -r $(CLEANFILES)
.PHONY: clean

lint: ## Runs lintings and diagnostics
	$(CLJ) -T:project lint
.PHONY: lint

deps: ## Show deps tree
	$(CLJ) -Stree
.PHONY: deps

outdated: ## Run antq (aka 'ancient') task on all modules
	$(CLJ) -M:outdated
.PHONY: outdated

release: ## Release jar modules & tag versions
	git config --global --add safe.directory '*'
	$(CLJ) -T:project release
.PHONY: release

version: ## Output project version
	@echo $(shell cat $(ROOT_DIR)/VERSION)

compile: ## Compile namespaces
	$(CLJ) -T: project check

.SILENT: info
info: ## Show repo information
	@echo -e "version:\t" $(shell cat $(ROOT_DIR)/VERSION)
	@echo -e "git-ref:\t" $(shell git rev-parse HEAD)

help:  ## Display this help
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make \033[36m\033[0m\n"} /^[a-zA-Z_-]+:.*?##/ { printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2 } /^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 5) } ' $(MAKEFILE_LIST)
