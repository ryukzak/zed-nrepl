# zed-nrepl

A simple nREPL bridge for Zed editor that enables Clojure REPL integration without plugins.

Status: proof-of-concept

## What is zed-nrepl?

`zed-nrepl` provides an HTTP bridge between Zed editor and nREPL, allowing you to evaluate Clojure code directly from Zed using simple tasks.

This approach lets you enjoy REPL-driven development without the need for a complex plugin system (but we wait for it).

## Getting Started

### 1. Add the dependency

Include `zed-nrepl` in your project's `deps.edn` file:

```clojure
{:aliases
 {:zed-repl
  {:extra-deps {io.github.ryukzak/zed-nrepl
                {:git/url "https://github.com/ryukzak/zed-nrepl.git"
                 :git/sha "___________________________________"}}
   :main-opts ["-m" "zed-nrepl.core"]}}}
```

### 2. Launch the zed-nrepl

Run this command in your project directory:

```bash
clojure -M:zed-repl
```

This will:
- Configure Zed by adding tasks to `.zed/tasks.json` (existing files are automatically backed up)
- Start an nREPL server connected to your project
- Create the HTTP bridge server on port 3000

### 3. Use in Zed

Now you can evaluate Clojure code:
1. Open any Clojure file in your project
2. Select the code you want to evaluate
3. Press `Cmd+Shift+R` (or `Ctrl+Shift+R` on Windows/Linux) and select `Eval selected code (zed-repl)`
4. View results in the panel that appears

### 4. Customize keyboard shortcuts (recommended)

Add this to your Zed key bindings configuration for a more efficient workflow:

```json
[
  {
    "context": "Workspace",
    "bindings": {
      "ctrl-x ctrl-e": ["task::Spawn", { "task_name": "Eval selected code (zed-repl)" }]
    }
  }
]
```

## License

BSD 2-Clause License. See [LICENSE](LICENSE) for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request or open an issue to discuss improvements.

## TODO:

- [ ] Evaluate current file
- [ ] Evaluate current top-level expression.
- [ ] Evaluate current/previous symbol/sexpr..
