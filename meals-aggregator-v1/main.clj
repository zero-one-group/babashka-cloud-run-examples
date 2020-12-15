(ns main
 (:require
   [clojure.edn :as edn]
   [cheshire.core :as cheshire]
   [org.httpkit.client :as http-client]
   [org.httpkit.server :as http-server]))

(def env
  (edn/read-string (slurp "config.edn")))

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

(defn fill-meals-calories [meal]
  (if (:calories meal)
    meal
    (assoc meal :calories (estimate-calories (:description meal)))))

(defn aggregate-meals [meals]
  (let [filled (mapv fill-meals-calories meals)
        agged (->> filled
                   (group-by :date)
                   (mapv
                     (fn [[date meals]]
                      (let [total-calories (apply + (map :calories meals))
                            entry {:total_calories total-calories
                                   :meals (map #(dissoc % :date) meals)}]
                        [date entry])))
                   (into {}))]
    (cheshire/generate-string agged)))

(defn -main []
  (http-server/run-server
    (fn [request]
      (if (= (:uri request) "/aggregate-meals")
        (let [meals (-> request :body slurp (cheshire/parse-string true))]
          {:body (aggregate-meals meals)
           :headers {"Content-Type" "application/json"}})
        {:body (str request)}))
    {:port 8080})
  @(promise))

(-main)
