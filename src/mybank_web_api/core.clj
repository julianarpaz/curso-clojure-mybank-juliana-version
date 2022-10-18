(ns mybank-web-api.core
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.test :as test-http]
            [clojure.pprint :as pp])
  (:gen-class))

(defonce contas (atom {:1 {:saldo 100}
                   :2 {:saldo 200}
                   :3 {:saldo 300}}))

(defn get-saldo [request]
  (let [id-conta (-> request :path-params :id keyword)]
    {:status 200 :body {:saldo (id-conta @contas "conta inválida!")}}))

(defn make-deposit [request]
  (let [id-conta (-> request :path-params :id keyword)
        valor-deposito (-> request :body slurp parse-double)]
        (swap! contas (fn [m] (update-in m [id-conta :saldo] #(+ % valor-deposito))))
    {:status 200 :body {:id-conta id-conta
                        :novo-saldo (id-conta @contas)}}))

(defn withdrawl [request]
  (let [id-conta (-> request :path-params :id keyword)
        valor-saque (-> request :body slurp parse-double)]
        (swap! contas (fn [m] (update-in m [id-conta :saldo] #(- % valor-saque))))
    {:status 200 :body {:id-conta id-conta
                        :novo-saldo (id-conta @contas)}}))

(def routes
  (route/expand-routes
    #{["/saldo/:id" :get get-saldo :route-name :saldo]
      ["/deposito/:id" :post make-deposit :route-name :deposito]}))

(defn create-server []
  (http/create-server
    {::http/routes routes
     ::http/type   :jetty
     ::http/port   8890
     ::http/join?  false}))

(defonce server (atom nil))

(defn start []
  (reset! server (http/start (create-server))))

(defn stop [server]
  (http/stop server))

(defn reset-server
  "If no args are given returns a server created,
  but if the service-map - an initialized service map returned
  by `create-server`- is given stops it. Either way, in case of success
  returns `service-map`"
  ([]
   (try
     (start)
        (catch Exception e (str "Unable to start server: " (.getMessage e)))))
  ([server]
   (try
     ((stop server))
     (catch Exception e (str "Unable to stop server: " (.get e))))))

(defn test-request [server verb url]
  (test-http/response-for (::http/service-fn @server) verb url))
(defn test-post [server verb url body]
  (test-http/response-for (::http/service-fn @server) verb url :body body))

(comment
  (start)
  (http/stop @server)
  (reset-server)
  (reset-server @server)

  (test-request server :get "/saldo/1")
  (test-request server :get "/saldo/2")
  (test-request server :get "/saldo/3")
  (test-request server :get "/saldo/4")

  (test-post server :post "/deposito/2" "863.99")
)


