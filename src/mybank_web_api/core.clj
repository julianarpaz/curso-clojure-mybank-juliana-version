(ns mybank-web-api.core
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.test :as test-http]
            [io.pedestal.interceptor :as i]
            [clojure.pprint :as pp])
  (:gen-class))

(defonce server (atom nil))

;1 - Tratamento de conta inválida/inexistente no deposito. Retornar o status http de erro e mensagem no body.
;
;2 - Implementar funcionalidade saque
;
;3 - Criar reset do servidor (tenta stop e tenta start) e demonstrar no mesmo repl antes e depois do tratamento de erro no ex. 1

(defonce contas (atom {:1 {:saldo 100}
                       :2 {:saldo 200}
                       :3 {:saldo 300}}))

(defn add-contas-atom [context]
  (update context :request assoc :contas contas))

(def contas-interceptor
  {:name  :contas-interceptor
   :enter add-contas-atom})

(defn get-saldo [request]
  (let [id-conta (-> request :path-params :id keyword)]
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body (id-conta @contas "conta inválida!")}))

(defn make-deposit [request]
  (let [id-conta (-> request :path-params :id keyword)
        valor-deposito (-> request :body slurp parse-double)
        SIDE-EFFECT! (swap! contas (fn [m] (update-in m [id-conta :saldo] #(+ % valor-deposito))))]
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body {:id-conta   id-conta
                        :novo-saldo (id-conta @contas)}}))

(defn make-withdraw [request]
  (let [conta (:conta request)
        valor (-> request :body slurp parse-double)
        SIDE-EFFECT! (swap! contas (fn [m] (update-in m [(:id conta) :saldo] #(- % valor))))]
    {:status  200
     :headers {"Content-Type" "text/plain"}
     :body    {:id-conta   (:id conta)
               :novo-saldo ((:id conta) @contas)}}))

(def validate-conta-existe
  {:name  ::validate-conta-existe
   :enter (fn [context]
            (let [id (-> (:request context) :path-params :id keyword)
                  saldo-usuario (get @contas id)]
              (if saldo-usuario
                (update context :request assoc :conta (merge saldo-usuario {:id id}))
                (assoc context :response {:status  400
                                          :headers {"Content-Type" "text/plain"}
                                          :body    {:erro  "conta não existe"
                                                    :conta id}}))))})

(def routes
  (route/expand-routes
    #{["/saldo/:id" :get get-saldo :route-name :saldo]
      ["/deposito/:id" :post [validate-conta-existe make-deposit] :route-name :deposito]
      ["/saque/:id" :post [validate-conta-existe make-withdraw] :route-name :saque]}))


(def service-map-simple {::http/routes routes
                         ::http/port   9999
                         ::http/type   :jetty
                         ::http/join?  false})

(def service-map (-> service-map-simple
                     (http/default-interceptors)
                     (update ::http/interceptors conj (i/interceptor contas-interceptor))))

(defn create-server []
  (http/create-server
    service-map))

(defn start []
  (reset! server (http/start (create-server))))

(defn test-request [server verb url]
  (test-http/response-for (::http/service-fn @server) verb url))
(defn test-post [server verb url body]
  (test-http/response-for (::http/service-fn @server) verb url :body body))

(comment
  (start)
  (http/stop @server)


  (test-request server :get "/saldo/1")
  (test-request server :get "/saldo/2")
  (test-request server :get "/saldo/3")
  (test-request server :get "/saldo/4")
  (test-post server :post "/deposito/1" "199.93")
  (test-post server :post "/deposito/4" "325.99")
  (test-post server :post "/saque/1" "1")

  ;curl http://localhost:9999/saldo/1
  ;curl -d "199.99" -X POST http://localhost:9999/deposito/1
  )
