(ns mybank-web-api.bank
  (:require [schema.core :as s]
            [io.pedestal.interceptor :as i]))


(s/defschema IdConta s/Keyword)
(s/defschema Contas {s/Keyword {:saldo Number}})
(s/defschema SaldoResult (s/maybe {:saldo Number}))


(s/defschema Context {s/Any s/Any})

(s/defschema Response {s/Any     s/Any
                       :response {:body   s/Any
                                  :status s/Int
                                  s/Any   s/Any}})

(defn is-a-valid-number? [string]
  (double? (parse-double string)))

(defn positive? [string]
  (-> string bigdec (> 0)))

(defn sanitizer [string]
  "Determinates if a string passed as input is a positive number and case it is
  returns its content as a BigDecimal, case it's not "
  (if (and (is-a-valid-number? string) (positive? string))
    (bigdec string)))

(s/defn ^:always-validate get-saldo :- SaldoResult
  [id-conta :- IdConta
   contas :- Contas]
  (get contas id-conta))


(s/defn ^:always-validate get-saldo-interceptor :- Response
  [context :- Context]
  (let [id-conta (-> context :request :path-params :id keyword)
        contas (-> context :contas)
        saldo (get-saldo id-conta @contas)]
    (assoc context :response {:status  200
                              :headers {"Content-Type" "text/plain"}
                              :body    saldo})))

(s/defschema ValorDeposito (s/pred number?))

(s/defschema ContasAtom (s/pred #(instance? clojure.lang.Atom %)))
(s/defschema DepositoResult {:id-conta s/Keyword
                             :novo-saldo s/Num})

(s/defn ^:always-validate make-deposit! :- Contas
  [id-conta :- IdConta
   contas :- ContasAtom
   valor-deposito :- ValorDeposito]
  (swap! contas (fn [m] (update-in m [id-conta :saldo] #(+ % valor-deposito)))))


(defn make-deposit-interceptor [context]
  (let [id-conta (-> context :request :path-params :id keyword)
        contas (-> context :contas)
        valor-deposito (-> context :valor)
        _ (make-deposit! id-conta contas valor-deposito)
        novo-saldo (id-conta @contas)]
    (assoc context :response {:status  200
                              :headers {"Content-Type" "text/plain"}
                              :body    {:id-conta   id-conta
                                        :novo-saldo novo-saldo}})))
(def validate-value
  (i/interceptor {:name  :validate-value
    :enter (fn [context]
             (let [value (-> context :request :body slurp)
                   value-sanitizado (sanitizer value)]
               (if value-sanitizado
                 (assoc context :valor value-sanitizado)
                 (assoc context :response {:status  400
                                           :headers {"Content-Type" "text/plain"}
                                           :body    {:erro  "Valor inválido."
                                                     :valor value}}))))}))

(def validate-conta-existe
  (i/interceptor {:name  :validate-conta-existe
    :enter (fn [context]
             (let [id (-> (:request context) :path-params :id keyword)
                   contas (-> context :contas)
                   conta (get @contas id)]
               (if conta                                    ;; { :saldo nil } mesmo que saldo nil, retorna true
                 (assoc context :conta (merge conta {:id id}))
                 (assoc context :response {:status  400
                                           :headers {"Content-Type" "text/plain"}
                                           :body    {:erro  "conta não existe"
                                                     :conta id}}))))}))
