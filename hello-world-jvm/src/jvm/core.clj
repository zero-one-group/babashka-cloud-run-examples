(ns jvm.core
 (:require
  [org.httpkit.server :as http])
 (:gen-class))

(defn -main [& _]
  (http/run-server
   (constantly {:body "Hello from JVM Clojure!"})
   {:port 8080})
 @(promise))
