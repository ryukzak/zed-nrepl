(ns zed-nrepl.snippets
  "some code snippets to test how zed-repl works")

(defn long-polling []
  (println 1)
  (Thread/sleep 700)
  (println 2)
  (Thread/sleep 700)
  (println 3)
  (Thread/sleep 700)
  (println 4)
  (Thread/sleep 700)
  (println 5)
  [1 2 3 4 5])

*ns*

(long-polling)

(throw (ex-info "Test exception" {:key "value"}))

(comment)
*ns*
*e
