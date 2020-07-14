(defproject compojure-throttle "0.1.9"

  :description "Throttling middleware for compojure"

  :url "http://github.com/whostolebfrog/compojure-throttle"

  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[clj-time "0.11.0"]
                 [functionalbytes/clj-ip "0.9.0"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/core.cache "1.0.207"]
                 [environ "1.0.1"]]

  :lein-release {:deploy-via :clojars}

  :profiles {:dev  {:dependencies [[midje "1.9.9"]]
                    :plugins      [[lein-midje "3.2.1"]
                                   [lein-release "1.0.5"]
                                   [lein-environ "1.1.0"]]
                    :env          {:service-compojure-throttle-lax-ips "127.0.0.1/32"}}})
