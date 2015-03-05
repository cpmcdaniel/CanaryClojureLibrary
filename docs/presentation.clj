;; imports
(ns net.canarymod.plugin.lang.clojure.presentation
  (:import [net.canarymod.plugin Plugin PluginDescriptor Priority PluginListener]
           [net.canarymod.exceptions InvalidPluginException]
           [net.canarymod Canary Translator]
           [net.canarymod.logger Logman]
           [net.canarymod.hook Dispatcher Hook]
           [net.canarymod.commandsys CanaryCommand TabCompleteDispatch
            DynamicCommandAnnotation]
           [net.canarymod.config Configuration])
  (:use [clojure.pprint :only [pprint]]))

;; Member access
;;(.instanceMember obj args*)
;;(.instanceMember Classname args*)
;;(Classname/staticMethod args*)
;;Classname/staticField            ;; not a function!

;; examples
(.toUpperCase "cow")
;; -> "COW"

;;(.getLogman plugin)

(.getName Logman)
;; -> "net.canarymod.logger.Logman"

(System/getProperty "user.home")
;; -> "/Users/cmcdaniel"

Math/PI
;; -> 3.141592653589793


;; dot special form expansion
;; happens at macro expansion time
(. "cow" toUpperCase)              ;; <= (.toUpperCase "cow")
(. (identity String) getName)      ;; <= (.getName String)
(. System getProperty "user.home") ;; <= (System/getProperty "user.home")
(. Math PI)                        ;; <= Math/PI


;; dot dot
;; (.. instance-expr member+)
;; (.. Classname member+)
;; member => fieldName-symbol or (instanceMethodName-symbol args*)

(.. System (getProperties) (get "os.name"))
;; -> "Mac OS X"

;; expands to
(. (. System (getProperties)) (get "os.name"))

;; similar to
(-> (System/getProperties) (.get "os.name"))


;; instantiation
(java.io.File. "/etc/cassandra" "cassandra.yaml")

;; expands to
(new java.io.File "/etc/cassandra" "cassandra.yaml")

;; check type
(instance? String "foo")
;; -> true



;; doto
(doto (new java.util.HashMap)
  (.put "a" 1)
  (.put "b" 2)
  (.put "c" 3)) ;; and returns the map

(.put (new java.util.HashMap) "a" 1) ;; returns nil

(let [my-hash-map (new java.util.HashMap)]
  (.put my-hash-map "a" 1)
  (.put my-hash-map "b" 2)
  (.put my-hash-map "c" 3)
  my-hash-map) ;; explicit return

;; type hints
(defn last-char [s]
  (.hashCode (.charAt s (dec (.length s)))))

(defn last-char2 [^String s]
  (.hashCode (.charAt s (dec (.length s)))))

(time (reduce + (map last-char (repeat 1000000 "ABC"))))
;; "Elapsed time: 7322.658 msecs"

(time (reduce + (map last-char2 (repeat 1000000 "ABC"))))
;; "Elapsed time: 2753.045 msecs"

;; (last-char 10)
;; (last-char2 10)

;; useful for optimizing performance:
(set! *warn-on-reflection* true)
(defn first-char [s] (.charAt s 0))

;; Reflection warning, /.../presentation.clj:114:22 - call to method charAt
;; can't be resolved (target class is unknown)



;; primitive coercion
(int 12345)
(long 12345)
(float 123.5)
(double 123.5)
(boolean true)
(char \A)
(bigdec 1234.5)
(bigint 1234)
;; and more

;; arithmetic overloaded to support primitives
(+ (int 1) (int 2))


;; implementing interfaces and extending classes

;; using a reified FileFilter implementation to obtain only directory files
(.listFiles (java.io.File. ".")
  (reify
    java.io.FileFilter
    (accept [this f]
      (.isDirectory f))))

;; proxy an existing class
(proxy [Plugin] []
  (enable  [this] (println "Enabling!"))
  (disable [this] (println "Disabling!")))

;; gen-class generates compiled bytecode
(gen-class
 :name "hello.HelloCljPlugin"
 :extends net.canarymod.plugin.Plugin
 :state "state"
 :init "init"
 :prefix "plugin-" ;; prefix is - by default
 :constructors {[net.canarymod.plugin.PluginDescriptor] []})

;; returns super constructor args and initial state
(defn plugin-init [^desc]
  [[] (atom {})])

(defn plugin-enable [this]
  (println "Enabling!"))

(defn plugin-disable [this]
  (println "Disabling!"))

;; can also use :gen-class in the ns macro



;;;;;;; Less relevant to blowing up cows...


;; arrays
;; primitive type-specific array creation
(int-array [1 2 3])
(int-array 5)       ;; initially nulls

;; array operations and mutations
(def arr (int-array [1 2 3]))
(aget arr 0)        ;; -> 1
(alength arr)       ;; -> 3
(aset-int arr 2 45)
(vec arr)           ;; -> [1 2 45]

;; arrays of reference types
(make-array Thread 4)
(def th-arr (make-array Thread 2))
(vec th-arr) ;; -> [nil, nil]
(aset th-arr 0 (Thread.))
(vec th-arr) ;; [#<Thread Thread[Thread-35,5,main]>, nil]


;; create array from existing collection
;; recommended use only when calling code that requires arrays
(to-array [1 2 3 4])
(into-array String ["a" "b"])
(amap ^ints arr   ;; with array type hint
      idx
      new-array
      (+ (int 1) (aget ^ints arr idx)))

;; most seq functions work fine on arrays
(filter odd? (int-array [1 2 3])) ;; -> (1 3)
(contains? (int-array [1 2 3]) 4) ;; -> false


;; Clojure maps are java.util.Maps
(instance? java.util.Map {})       ;; -> true

;; Clojure lists and vectors are java.util.Lists
(instance? java.util.List (list))  ;; -> true
(instance? java.util.List [])      ;; -> true

;; Clojure sets are java.util.Sets
(instance? java.util.Set #{})      ;; -> true

;; all Clojure fns are java.lang.Runnables
(instance? java.lang.Runnable inc) ;; -> true

;; however, only useful for threading when no args
(defn print-thread-name []
  (println (.. Thread currentThread getName)))
(doto (Thread. print-thread-name)
  .start)

;; convert Java bean to a Clojure map
;; property names become keywords
(bean java.awt.Color/black)
(comment ;; ->
  {:transparency 1,
   :red 0,
   :green 0,
   :class java.awt.Color,
   :blue 0,
   :alpha 255,
   :RGB -16777216})
