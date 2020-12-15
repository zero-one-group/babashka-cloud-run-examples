(ns main
  (:require
   [org.httpkit.server :as http]))

(defn -main []
  (http/run-server
   (constantly {:body "Hello from Babashka!"})
   {:port 8080})
  @(promise))

(-main)
