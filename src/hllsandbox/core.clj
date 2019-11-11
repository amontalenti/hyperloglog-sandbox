(ns hllsandbox.core
  (:import
    [java.util HashSet]
    [java.nio ByteBuffer]
    [java.io ByteArrayInputStream ByteArrayOutputStream]
    [org.apache.lucene.util BytesRef LongBitSet]
    [org.apache.lucene.util.packed PackedInts]
    [com.carrotsearch.hppc BitMixer]
    [org.elasticsearch.common.util BigArrays ByteArray ByteUtils IntArray]
    [org.elasticsearch.common.hash MurmurHash3 MurmurHash3$Hash128]
    [org.elasticsearch.common.io.stream InputStreamStreamInput OutputStreamStreamOutput]
    [org.elasticsearch.search.aggregations.metrics.cardinality HyperLogLogPlusPlus]
    )
  (:require [clojure.reflect :as cr]
            [clojure.pprint :as pp]
            [clojure.string :as string]
            [clojure.data.codec.base64 :as b64]
            )
  (:gen-class))

(defrecord HLL
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
  ;; The `sketch` part is obviously the most interesting. In case of
  ;; HLL, it'll likely be bitpacked registers representing the max
  ;; run of zeros in the hashed values' bitstrings. In case of LC,
  ;; it'll be a bitpacked "linear count" of the hashed values. I'm
  ;; not sure if the sketch should have more structure than this.
  [binformat sketch precision])

(defn random-uuid []
  ;; Generate a UUID as a string
  (str (java.util.UUID/randomUUID)))

(defn make-uuids [n]
  (for [i (take n (range))]
    (random-uuid)))

(defn make-uuids [n]
  ;; cleaner version of make-uuids
  (take n (repeatedly random-uuid)))

(defn hll-new [p]
  ;; Construct an HLL using the main Java class
  (HyperLogLogPlusPlus. p BigArrays/NON_RECYCLING_INSTANCE 0))

(defn str2bytes [s]
  ;; in Java:
  ;; BytesRef bytes = new BytesRef(value.toString());
  (BytesRef. (str s)))

(defn bytes2hash [b]
  ;; in Java:
  ;; hash = MurmurHash3.hash128(bytes.bytes, bytes.offset,
  ;;   bytes.length, 0, new MurmurHash3.Hash128()).h1;
  (.h1 (MurmurHash3/hash128
    (.bytes b)
    (.offset b)
    (.length b)
    0
    (MurmurHash3$Hash128.)
  ))
)

(defn bitmix [uuid]
  ;; "Bit Mixing" is necessary due to something called
  ;; "the avalanche effect", which is described here:
  ;; http://zimbry.blogspot.com/2011/09/better-bit-mixing-improving-on.html
  (BitMixer/mix64 uuid))

(defn long2bytes [l]
  ;; Use ByteBuffer to convert a `long` to a `byte[]`, which we then
  ;; convert into a `vector` for readability
  (vec
    (.array (.putLong (ByteBuffer/allocate (/ Long/SIZE Byte/SIZE)) l))))

(defn hll-bits [item]
  ;; Converts `item` string into murmur3-hashed and bitmixed bits,
  ;; making it ready for HLL collection.
  (let [bytes (str2bytes item)
        hash  (bytes2hash bytes)
        mix   (bitmix hash)]
    ;(println item)
    ;(println bytes)
    ;(println mix)
    ;(println (long2bytes mix))
  mix))

(defn hll-collect [hll item]
  ;; Collects an item into a given hll; can use with `(partial hll)`
  (.collect hll 0 (hll-bits item)))

(defn hll-serialize [hll]
  ;; Serialize an HLL into a byte[]
  (let [baos (ByteArrayOutputStream.)
        osso (OutputStreamStreamOutput. baos)]
    (.writeTo hll 0 osso)
    (.toByteArray baos)))

(defn hll-deserialize [buf]
  ;; Deserialize a byte[] into an HLL
  (let [bais (ByteArrayInputStream. buf)
        issi (InputStreamStreamInput. bais)]
    (HyperLogLogPlusPlus/readFrom issi BigArrays/NON_RECYCLING_INSTANCE)))

(defn print-class-table [obj]
  ;; Reflection utility to print a class's members
  (println (macroexpand '(->> obj cr/reflect :members pp/print-table)))
  (println (->> obj cr/reflect :members pp/print-table)))

(defn -main [& args]
  (let [devices (make-uuids 10000)
        hll (hll-new 14)
        ]
    (println "HLL simulation built; device count =>")
    (println (count devices))
    (println)
    (println "Showing some devices =>")
    (doall (map pp/pprint (take 5 devices)))
    (println)
    (println "Showing 'mock' serialization format =>")
    (pp/pprint (HLL. "hll_v1" "3aFde4" 14))
    (pp/pprint (HLL. "lc_v1"  "4bGeF5" 14))
    (println)
    (println "Instantiating real ES HyperLogLogPlusPlus =>")
    (println "Cardinality of an empty HLL:" (.cardinality (hll-new 14) 0))
    (doall (map (partial hll-collect hll) devices))
    (println "Cardinality with" (count devices) "offered items: " (.cardinality hll 0))
    (println)
    (println "Testing Ser and DeSer =>")
    (let [ser (hll-serialize hll)
          deser (hll-deserialize ser)]
        (println (count ser) "serialized bytes")
        (println (.cardinality deser 0) "deserialized cardinality")
    )
  )
)
