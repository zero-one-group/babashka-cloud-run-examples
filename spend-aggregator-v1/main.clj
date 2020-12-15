(ns main
 (:require
   [cheshire.core :as cheshire]
   [org.httpkit.client :as http-client]
   [org.httpkit.server :as http-server]))

(defn lookup-idr-rate [date currency]
  (let [response (http-client/get
                  (str "https://api.exchangeratesapi.io/" date)
                  {:query-params {"base" currency "symbols" "IDR"}})]
    (-> @response :body (cheshire/parse-string true) :rates :IDR)))

(defn spend->idr-amount [{:keys [currency amount date]}]
  (if (= currency "IDR")
    amount
    (* amount (lookup-idr-rate date currency))))

(defn aggregate-spends [spends]
  (let [agged (->> spends
                   (group-by :date)
                   (mapv
                     (fn [[date spends]]
                      (let [idr-amounts (map spend->idr-amount spends)
                            summary {:total_spend (apply + idr-amounts)
                                     :spends spends}]
                        [date summary])))
                   (into {}))]
    (cheshire/generate-string agged)))

(defn -main []
  (http-server/run-server
    (fn [request]
      (if (= (:uri request) "/aggregate-spends")
        (let [spends (-> request :body slurp (cheshire/parse-string true))]
          {:body (aggregate-spends spends)
           :headers {"Content-Type" "application/json"}})
        {:body (str request)}))
    {:port 8080})
  @(promise))

(-main)
