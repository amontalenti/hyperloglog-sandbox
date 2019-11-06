(ns hllsandbox.core
  (:import
    [com.carrotsearch.hppc BitMixer]
    [org.elasticsearch.common.util BigArrays ByteArray ByteUtils IntArray]
    [java.util HashSet]
    [org.apache.lucene.util BytesRef LongBitSet]
    [org.apache.lucene.util.packed PackedInts]
    [org.elasticsearch.search.aggregations.metrics.cardinality HyperLogLogPlusPlus])
  (:gen-class))

;; Notes from reading HyperLogLogPlusPlus.java from Elasticsearch:
;;
;; - Precision min/max is 4 - 18
;; - There are also "thresholds" which provide a convenience over
;;   the precision values; the set looks like [10, 20, ..., 350000]
;;   and they map to the precision values.
;; - When the HLL cardinality is small, "linear counting" is used,
;;   but with a small modification of using a hash table for LC.
;; - According to acoyler analysis, at p = 14, linear counting performs
;;   better for cardinalities between 0 - 50k, and then HLL starts to
;;   perform much better, especially with bias correction. See this:
;;   https://adriancolyer.files.wordpress.com/2016/03/hll-fig-3.png
;;
;; To quote Adrian: "The end result is that for an important range
;; of cardinalities -- roughly between 18k and 61k p = 14, the
;; error is much less than that of the original HyperLogLog." ...
;; "Notice that linear counting still beats the bias-corrected estimate
;; for small n. For precision 14 (p = 14) the error curves intersect at
;; around n = 11.5k. Therefore linear counting is used below 11.5k, and
;; bias corrected raw estimates above."

;; Represents an HLL, with `sketch` being the serialized form,
;; `binformat` being the binary format used, and `precision` being
;; the precision of the HLL, which affects its space usage and
;; its serialization format.
;;
;; Example values:
;; - binformat: "hll_v1", "lc_v1"
;; - sketch: "f3e4d5"
;; - precision: 14
;;
(defrecord HLL
  [binformat sketch precision])

;; Represents a device
(defrecord Device
  [uuid])

;; Generate a UUID as a string
(defn make-uuid []
  (.toString (java.util.UUID/randomUUID)))

;; Make a bunch of `Device` records
(defn make-devices [n]
  (for [i (take n (range))] (Device. (make-uuid))))

(defn make-hll [p]
    (HyperLogLogPlusPlus. p BigArrays/NON_RECYCLING_INSTANCE 0))

(defn -main [& args]
  (let [devices (make-devices 100)]
    (println "HLL simulation built")
    (println (count devices))
    (println (take 10 devices))
    (println (HLL. "hll_v1" "3aFde4" 14))
    (println (HLL. "lc_v1"  "4bGeF5" 14))
    (println "Instantiating real ES HyperLogLogPlusPlus")
    (println (.cardinality (make-hll 14) 0))
  )
)
