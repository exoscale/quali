(ns exoscale.quali-test
  {:clj-kondo/config '{:linters {:unresolved-symbol {:level :off}}}}
  (:require [model.user                      :as-alias user]
            [model.account                   :as-alias account]
            [exoscale.quali                  :as quali]
            [clojure.data                    :as d]
            [clojure.spec.alpha              :as s]
            [clojure.test.check.properties   :as prop]
            [clojure.test.check.results      :as results]
            [clojure.test.check.clojure-test :refer [defspec]]))

(s/def ::id          nat-int?)
(s/def ::name        string?)
(s/def ::description string?)

(s/def ::user/id   ::id)
(s/def ::user/name ::name)
(s/def ::user/size nat-int?)
(s/def ::user      (s/keys :req [::user/id ::user/name ::user/size]
                           :opt [::description]))

(s/def ::account/id           ::id)
(s/def ::account/name         ::name)
(s/def ::account/live-users   (s/coll-of ::user))
(s/def ::account/total-credit int?)
(s/def ::account/used-credit  int?)
(s/def ::account              (s/keys :req [::account/id
                                            ::account/name
                                            ::account/live-users]
                                      :opt [::description]
                                      :req-un [::account/total-credit]
                                      :opt-un [::account/used-credit]))

(defn rcompare
  [m1 m2]
  (reify results/Result
    (pass? [_] (= m1 m2))
    (result-data [_]
      (let [[e r c] (d/diff m1 m2)]
        {:expected e :result r :common c}))))

(defspec encode-decode-test-1 100
  (prop/for-all
   [m (s/gen ::account)]
   (rcompare m (quali/decode ::account (quali/encode m)))))

(defspec encode-decode-test-2 100
  (prop/for-all
   [m (s/gen ::user)]
   (rcompare m (quali/decode ::user (quali/encode m)))))
