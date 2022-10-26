(ns mybank-web-api.routing
  (:require [mybank-web-api.bank :as bank]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as i]
            [com.stuartsierra.component :as component]))


(def routes
  (route/expand-routes
    #{["/saldo/:id" :get [bank/validate-conta-existe
                          (i/interceptor {:name  :get-saldo
                                          :enter bank/get-saldo-interceptor})] :route-name :saldo]
      ["/deposito/:id" :post [bank/validate-conta-existe
                              bank/validate-value
                              (i/interceptor {:name  :make-deposit
                                              :enter bank/make-deposit-interceptor})] :route-name :deposito]
      ["/saque/:id" :post [bank/validate-conta-existe
                           bank/validate-value
                           (i/interceptor {:name  :make-deposit
                                           :enter bank/make-deposit-interceptor})] :route-name :saque]}))



(defrecord Routes []
  component/Lifecycle
  (start [this]
    (assoc this :routes routes))
  (stop [this]
    (dissoc this :routes))
  )

(defn new-routes []
  (->Routes))