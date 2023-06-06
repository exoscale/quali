quali: spec helper to (de-)qualify maps
=======================================

[![cljdocbadge](https://cljdoc.xyz/badge/exoscale/quali)](https://cljdoc.org/d/exoscale/quali/CURRENT/api/exoscale.quali)
[![Clojars Project](https://img.shields.io/clojars/v/com.exoscale/quali.svg)](https://clojars.org/exoscale/quali)
![Test workflow](https://github.com/exoscale/quali/actions/workflows/main.yml/badge.svg)

**quali** is a helper library for spec which helps go from standard
JSON shapes to spec'ed maps.  The library can be thought of as a pre
and post processor for JSON serialization.

The library has a few opinions:

- JSON shapes have no namespace indication and use snake case by
  default
- JSON objects are expected to have a corresponding Clojure spec
- Objects decoded from JSON to Clojure data will only keep known
  fields

## Installation

**quali** is available on clojars: https://clojars.org/exoscale/quali

## Usage

For full documentation refer to the documentation at
https://cljdoc.org/d/exoscale/quali/CURRENT

**quali** exposes two main functions:

- `encode`: walks payloads to generate data ready to be serialized to
  JSON objects
- `decode`: walks payloads to get back fully formed Clojure data

```
(s/def :my.model/id          nat-int?)
(s/def :my.model/name        string?)
(s/def :my.model/description string?)

(s/def ::metric-type   string?)
(s/def ::value         nat-int?)
(s/def ::metric        (s/keys :req-un [::metric-type ::value])
(s/def ::usage-metrics (s/coll-of ::metric))

(s/def :my.model/account (s/keys :req    [:my.model/id
                                          :my.model/name]
                                 :opt    [:my.model/description]
                                 :opt-un [::usage-metrics]))
```


## Caveats

At the moment there is currently no way to pass any options, the
opinions of the library are fully enforced. A later improvement could
add a user-supplied registry to control the renaming behavior.
