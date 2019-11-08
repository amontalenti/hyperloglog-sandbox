(ns hllsandbox.notes)

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
