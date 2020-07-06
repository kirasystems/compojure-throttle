(ns compojure-throttle.core
  (:require [clj.ip :as ip]
            [clj-time.core :as core-time]
            [clj-time.local :as local-time]
            [clojure.core.cache :as cache]
            [environ.core :refer [env]]))

(def ^:private defaults
  {:service-compojure-throttle-lax-ips       nil            ;keeping for more explicit defaults
   :service-compojure-throttle-ttl           1000
   :service-compojure-throttle-tokens        3
   :service-compojure-throttle-response-code 429})

(defn- ip-lax-subnet
  []
  (or (env :service-compojure-throttle-lax-ips)
      (defaults :service-compojure-throttle-lax-ips)))

(def in-lax-subnet? (if (ip-lax-subnet) 
                      (ip/compile (ip-lax-subnet))
                      (constantly false)))

(defn- prop
  [key]
  (Integer. (or (env key)
                (defaults key))))

(defn make-throttle-cache
  "Initialize a throttling cache with a given TTL and tokens.

   Pass a map containing keys :ttl and :tokens.

   This maintains all of the state for figuring out which request to throttle.
   (Yeah, it's just a TTL cache, but don't rely on that.)"
  [{:keys [ttl tokens]}]
  {:cache  (atom (cache/ttl-cache-factory {} :ttl ttl))
   :ttl    ttl
   :tokens tokens})

(defn reset-throttle-cache!
  "Testing helper that resets given throttling cache, to allow for tests to run reliably."
  [{:keys [cache ttl]}]
  (reset! cache (cache/ttl-cache-factory {} :ttl ttl)))

(def ^:private requests
  "Default cache for throttling requests globally."
  (make-throttle-cache {:ttl    (prop :service-compojure-throttle-ttl)
                        :tokens (prop :service-compojure-throttle-tokens)}))

(defn reset-cache
  "Testing helper that resets the content of the global cache - should allow
   tests to run from a known base"
  []
  (reset-throttle-cache! requests))

(defn- update-cache
  [cache id tokens]
  (swap! cache cache/miss id tokens))

(defn- record
  [tokens]
  {:tokens   tokens
   :datetime (local-time/local-now)})

(defn- throttle?
  [{:keys [cache ttl tokens]} id]
  (when-not (cache/has? @cache id)
    (update-cache cache id (record tokens)))
  (let [entry     (cache/lookup @cache id)
        spares    (int (/ (core-time/in-millis (core-time/interval
                                                 (:datetime entry)
                                                 (local-time/local-now)))
                          (/ ttl tokens)))
        remaining (+ (:tokens entry) spares)]
    (update-cache cache id (record (dec remaining)))
    (not (pos? remaining))))

(defn by-ip
  "A finder function that gets the IP from the request.

   This is used by default for `throttle` when no function is provided."
  [req]
  (:remote-addr req))

(defn throttle
  "Throttle incoming connections from a given source. By default this is based on IP.

   Throttling is controlled by both :service-compojure-throttle-enabled and 
   :service-compojure-throttle-lax-ips. If
   :service-compojure-throttle-enabled is false, throttling will still happen 
   to any IP not covered by :service-compojure-throttle-lax-ips.
  
   Optionally takes a second argument which is a function used to lookup the 'token'
   that determines whether or not the request is unique. For example a function that
   returns a user token to limit by user id rather than ip. This function should accept
   the request as its single argument.

   For full generality, this also takes an optional third argument, an instance
   of 'throttle-cache'. Create one using (make-throttle-cache ttl tokens) to
   have a separate throttler from the global one with custom settings."
  ([finder throttle-cache handler]
   (fn [req]
     (if (and (or (nil? (ip-lax-subnet))
                  (not (in-lax-subnet? (:remote-addr req))))
              (throttle? throttle-cache (finder req)))
       {:status (prop :service-compojure-throttle-response-code)
        :body   "You have sent too many requests. Please wait before retrying."}
       (handler req))))
  ([finder handler]
   (throttle finder requests handler))
  ([handler]
   (throttle by-ip handler)))
