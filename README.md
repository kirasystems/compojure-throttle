# Compojure-throttle

A Clojure Compojure middleware library for limiting the rate at which a user
can access a resource. Going over this rate will return an error response.

## Usage

Internal artifactory

    [compojure-throttle "0.1.9"]

Then use with 

    (:require [compojure-throttle.core :as throttler])

Then add to your middleware stack

    (def app
      (handler/site
       throttler/throttle
       main-routes))

Or add to a specific route

    (defroutes main-routes

      (throttler/throttle
        (POST "/data" req "OK")))

By default compojure-throttle throttles on IP. You can also pass an optional function
that allows it to throttle based on other attributes. This function should accept a
single argument (the request) and return a token that uniquely identifies the attribute you wish to throttle on.

For example, let's assume we have a :user entry in our request map that contains a
unique user id and that we want to throttle based on this.

    (throttler/throttle :user ...)

To configure the rate at which we throttle use two environment variables:

    SERVICE_COMPOJURE_THROTTLE_TTL=1000
    SERVICE_COMPOJURE_THROTTLE_TOKENS=3

TTL defines the period for which we are throttling e.g. 1000 milliseconds
TOKENS defines the number of tries a user is allowed within that period.
For example we might allow 3 responses a second.

This (token-bucket) approach allows us to handle small bursts in traffic without
throttling whilst still throttling sustained high traffic.

We can also configure the response code for throttled requests using:

    SERVICE_COMPOJURE_THROTTLE_RESPONSE_CODE=420
    
To disable throttling set:

    SERVICE_COMPOJURE_THROTTLE_LAX_IPS="subnet for disabling throttling" 
    
These settings are applied globally across all caches (see below).
    
## Even More Configurability

For full generality, you can create your own throttler-cache with custom TTL
and token settings, and pass it as a third argument to `throttle`. This allows 
you to have different TTL and token settings for different requests.

For example:

```
(defroutes main-routes

  ;; One request every 500ms, by :user
  (throttler/throttle
    :user
    (throttler/make-throttle-cache {:ttl 500 :tokens 1})
    (POST "/data" req "OK"))

  ;; Three requests every 2000ms, by IP
  (throttler/throttle
    throttler/by-ip
    (throttler/make-throttle-cache {:ttl 2000 :tokens 3})
    (POST "/data" req "OK")))
```

# Building #

    lein jar

# Testing #

    lein midje

# Author #

Benjamin Griffiths (whostolebenfrog)

## License

Copyright © 2012 Ben Griffiths

Distributed under the Eclipse Public License, the same as Clojure.
