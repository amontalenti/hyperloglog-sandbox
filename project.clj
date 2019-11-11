(defproject hllsandbox "0.1.0-SNAPSHOT"
  :description "sandbox for playing with HLL algorithms"
  :url "https://amontalenti.com/hll"
  :license {:name "MIT"
            :url "https://en.wikipedia.org/wiki/MIT_License"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.codec "0.1.1"]
                 [org.elasticsearch/elasticsearch "6.8.4"]
                 ]
  :main hllsandbox.core)
