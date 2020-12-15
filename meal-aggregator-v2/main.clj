(ns main
 (:require
   [clojure.edn :as edn]
   [cheshire.core :as cheshire]
   [org.httpkit.client :as http-client]
   [org.httpkit.server :as http-server]))

(def env
  (edn/read-string (slurp "config.edn")))

;; ---------------------------------------------------------------------------
;; Nutritionix
;; ---------------------------------------------------------------------------

(defn get-nutritionix-calories [query]
  (http-client/post
   "https://trackapi.nutritionix.com/v2/natural/nutrients"
   {:body (str "{\"query\":\"" query "\"}")
    :content-type :json
    :headers {"x-app-id" (:nutritionix-app-id env)
              "x-app-key" (:nutritionix-app-key env)}}))

(defn estimate-calories [query]
  (let [response (get-nutritionix-calories query)]
    (when (= 200 (:status @response))
      (let [foods (-> @response :body (cheshire/parse-string true) :foods)]
        (->> foods (mapv :nf_calories) (apply +))))))

(def cache-path
  ".nutritionix-cache.edn")

(defn maybe-read-cache []
  (try
    (edn/read-string (slurp cache-path))
    (catch Exception _ {})))

(defn memoised-estimate-calories [query]
  (let [cache         (maybe-read-cache)
        cached-result (cache query)]
    (if cached-result
      cached-result
      (let [result (estimate-calories query)
            new-cache (assoc cache query result)]
        (spit cache-path (pr-str new-cache))
        result))))

(defn fill-meals-calories [meal]
  (if (:calories meal)
    meal
    (assoc meal :calories (memoised-estimate-calories (:description meal)))))

;; ---------------------------------------------------------------------------
;; Middlewares
;; ---------------------------------------------------------------------------

(defn wrap-parse-json-body [handler]
  (fn [request]
    (let [body   (:body request)
          parsed (when body
                   (cheshire/parse-string (slurp body) true))]
      (handler (assoc request :body parsed)))))

;; ---------------------------------------------------------------------------
;; Handlers
;; ---------------------------------------------------------------------------

(defn echo [request]
  {:body (str request)})

(defn fill-calories [request]
  (let [meals (:body request)
        filled (mapv fill-meals-calories meals)]
    {:body (cheshire/generate-string filled)
     :headers {"Content-Type" "application/json"}}))

(defn aggregate-meals [request]
  (let [meals (:body request)
        filled (mapv fill-meals-calories meals)
        agged (->> filled
                   (group-by :date)
                   (mapv
                     (fn [[date meals]]
                      (let [total-calories (apply + (map :calories meals))
                            entry {:total_calories total-calories
                                   :meals (map #(dissoc % :date) meals)}]
                        [date entry])))
                   (into {}))]
    {:body (cheshire/generate-string agged)
     :headers {"Content-Type" "application/json"}}))

;; ---------------------------------------------------------------------------
;; Routes
;; ---------------------------------------------------------------------------

(def routes
  {"/fill-calories" {:post fill-calories}
   "/aggregate-meals" {:post aggregate-meals}})

;; ---------------------------------------------------------------------------
;; Service
;; ---------------------------------------------------------------------------

(defn app [request]
  (let [{:keys [uri request-method]} request
        handler (get-in routes [uri request-method] echo)]
    (handler request)))

(defn -main [{:keys [port handler middlewares]}]
  (let [wrapped (reduce #(%2 %1) handler middlewares)]
    (http-server/run-server wrapped {:port port}))
  (println (format "HTTP server started at port %s." port))
  @(promise))

(-main {:port 8080
        :handler app
        :middlewares [wrap-parse-json-body]})
