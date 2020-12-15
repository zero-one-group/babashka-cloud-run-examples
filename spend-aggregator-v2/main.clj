(ns main
 (:require
   [cheshire.core :as cheshire]
   [org.httpkit.client :as http-client]
   [org.httpkit.server :as http-server]))

;; ---------------------------------------------------------------------------
;; Exchange Rates
;; ---------------------------------------------------------------------------

(defn lookup-idr-rate [date currency]
  (let [response (http-client/get
                  (str "https://api.exchangeratesapi.io/" date)
                  {:query-params {"base" currency "symbols" "IDR"}})]
    (-> @response :body (cheshire/parse-string true) :rates :IDR)))

(defn spend->idr-amount [{:keys [currency amount date]}]
  (if (= currency "IDR")
    amount
    (* amount (lookup-idr-rate date currency))))

;; ---------------------------------------------------------------------------
;; Middlewares
;; ---------------------------------------------------------------------------

(defn wrap-parse-json-body [handler]
  (fn [request]
    (let [body   (:body request)
          parsed (when body
                   (cheshire/parse-string (slurp body) true))]
      (handler (assoc request :body parsed)))))

(defn wrap-json-response [handler]
  (fn [request]
    (let [response (handler request)
          headers  (:headers response)]
      (if (or (map? (:body response))
              (vector? (:body response)))
        (-> response
            (update :body cheshire/generate-string)
            (assoc :headers (assoc headers "Content-Type" "application/json")))
        response))))

;; ---------------------------------------------------------------------------
;; Handlers
;; ---------------------------------------------------------------------------

(defn default [_]
  {:body "Route not found!" :status 404})

(defn idr-spends [request]
  (let [spends (:body request)]
    {:body (mapv
             (fn [spend]
               (let [idr (spend->idr-amount spend)]
                 (assoc spend :amount idr :currency "IDR")))
             spends)}))

(defn aggregate-spends [request]
  (let [spends (:body request)
        agged (->> spends
                   (group-by :date)
                   (mapv
                     (fn [[date spends]]
                      (let [idr-amounts (map spend->idr-amount spends)
                            summary {:total_spend (apply + idr-amounts)
                                     :spends spends}]
                        [date summary])))
                   (into {}))]
    {:body agged}))

;; ---------------------------------------------------------------------------
;; Routes
;; ---------------------------------------------------------------------------

(def routes
  {"/idr-spends" {:post idr-spends}
   "/aggregate-spends" {:post aggregate-spends}})

;; ---------------------------------------------------------------------------
;; Service
;; ---------------------------------------------------------------------------

(defn app [request]
  (let [{:keys [uri request-method]} request
        handler (get-in routes [uri request-method] default)]
    (handler request)))

(defn -main [{:keys [port handler middlewares]}]
  (let [wrapped (reduce #(%2 %1) handler middlewares)]
    (http-server/run-server wrapped {:port port}))
  (println (format "HTTP server started at port %s." port))
  @(promise))

(-main {:port 8080
        :handler app
        :middlewares [wrap-parse-json-body
                      wrap-json-response]})
