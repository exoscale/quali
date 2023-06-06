(ns exoscale.quali
  "Helper library for spec to encode and decode JSON objects based on specs"
  (:require [camel-snake-kebab.core :as csk]
            [clojure.spec.alpha     :as s]
            [clojure.walk           :as walk]))

(declare decode)

(defn- spec-root
  [spec]
  (let [spec-def (or (some-> spec s/get-spec s/form)
                     (when (qualified-symbol? spec) spec)
                     (when (and (seq? spec) (symbol? (first spec))) spec)
                     (when (set? spec) spec))]
    (cond-> spec-def
      (qualified-keyword? spec-def)
      recur)))

(defn- gen-decode-or
  [[_ & pairs]]
  (fn [x]
    (let [xs (into []
                   (comp (partition-all 2)
                         (map #(decode (second %) x)))
                   pairs)]
      (or (reduce (fn [_ x']
                    (when (= x x')
                      (reduced x)))
                  nil
                  xs)
          (first xs)))))

(defn- gen-decode-and
  [[_ & [spec]]]
  (fn [x]
    (decode spec x)))

(defn- spec-keys-mapping
  [[_ & {:keys [req opt req-un opt-un]}]]
  (merge
   (into {}
         (comp (filter keyword?)
               (map #(vector (csk/->snake_case_keyword %)
                             [% %])))
         (flatten (concat req opt)))
   (into {}
         (comp (filter keyword?)
               (map #(vector (csk/->snake_case_keyword %)
                             [(keyword (name %)) %])))
         (flatten (concat req-un opt-un)))))

(defn- gen-decode-keys
  [spec-expr]
  (let [keys-mapping (spec-keys-mapping spec-expr)]
    (fn [x]
      (if (map? x)
        (reduce-kv (fn [m k [k' s]]
                     (let [v (get x k)]
                       (cond-> m
                         (some? v)
                         (assoc k' (decode s v)))))
                   {}
                   keys-mapping)
        x))))

(defn- gen-decode-coll-of
  [[_ spec & {:as _opts :keys [kind]}]]
  (fn [x]
    (if (coll? x)
      ;; either we have a `:kind` and coerce to that, or we just `empty` the
      ;; original
      (let [xs (into (condp = kind
                       `vector? []
                       `set? #{}
                       `coll? '()
                       `list? '()
                       (empty x))
                     (map #(decode spec %))
                     x)]
        (cond-> xs (list? xs)
                reverse))
      x)))

(defn- gen-decode-map-of
  [[_ kspec vspec & _]]
  (fn [x]
    (if (map? x)
      (into (if (record? x)
              (reduce-kv (fn [x k _] (assoc x k nil)) x x)
              (empty x))
            (map (fn [[k v]]
                   [(decode kspec k)
                    (decode vspec v)]))
            x)
      x)))

(defn- gen-decode-tuple
  [[_ & specs]]
  (fn [x]
    (if (sequential? x)
      (mapv #(decode %1 %2) specs x)
      x)))

(defn- gen-decode-merge
  [[_ & spec-forms]]
  (fn [x]
    (if (map? x)
      (reduce (fn [m spec-form]
                (into m
                      (keep (fn [[spec v]]
                              (when-not (= (get x spec) v)
                                [spec v])))
                      (decode spec-form x)))
              x
              spec-forms)
      x)))

(defn- gen-decode-nilable
  [[_ spec]]
  (fn [x]
    (when (some? x)
      (decode spec x))))

(def ^{:private true}
  decoder-forms
  {`s/or         gen-decode-or
   `s/and        gen-decode-and
   `s/nilable    gen-decode-nilable
   `s/coll-of    gen-decode-coll-of
   `s/every      gen-decode-coll-of
   `s/map-of     gen-decode-map-of
   `s/every-kv   gen-decode-map-of
   `s/keys       gen-decode-keys
   `s/merge      gen-decode-merge
   `s/tuple      gen-decode-tuple})

(defn- decoder-fn
  [spec]
  (let [spec-exp (spec-root spec)]
    (cond
       (qualified-ident? spec-exp)
       identity

       (sequential? spec-exp)
       (when-let [f (get decoder-forms (first spec-exp))]
         (f spec-exp)))))

(defn decode
  "Decode a Clojure data structure based on a spec. This is a
   no-op for most spec types instead of maps defined with `s/keys`.
   In the latter case, keys defined in the spec only are kept and
   translated to their expected shape."
  [spec x]
  (if-let [f (decoder-fn spec)]
    (f x)
    x))

(defn encode
  "Encode a data structure for JSON serialization. All maps are
  translated to use unqualified snake case keys to provide optimum
  compatibility with standard practices"
  [m]
  (let [f (fn [[k v]] [(cond-> k (keyword? k) csk/->snake_case_keyword) v])]
    (walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))

(s/fdef decode :args (s/cat :spec ::spec :x any?))
