(defproject jvm "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [http-kit "2.5.0"]]
  :main ^:skip-aot jvm.core
  :target-path "target/%s"
  :profiles
  {:uberjar {:omit-source true
             :aot :all
             :uberjar-name "app.jar"}})
