(ns ops-panel.core
  (:require [clj-ssh.ssh :as ssh]
            [clojure.pprint :as pp]
            [com.climate.claypoole :as pool]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [files not-found resources]]
            [hiccup.core :refer [html]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]))

(defn ssh [host cmd]
  (let [agent (ssh/ssh-agent {})
        session (ssh/session agent host {:username "lantern" :strict-host-key-checking :no})]
    (ssh/with-connection session (ssh/ssh session {:cmd cmd}))))

(defn pssh [hosts cmd]
  ;; XXX: reuse pool in concurrent requests, maybe cache it for some time or
  ;; even for the web server's lifetime
  (pool/pmap (min (count hosts) 50) #(ssh % cmd) hosts))

;; sente
(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn
              connected-uids]}
      (sente/make-channel-socket! sente-web-server-adapter {})]
  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def connected-uids connected-uids))

(defroutes handler

  (GET "/" req
    {:status 200
     :headers {"content-type" "text/html"}
     :body (html [:head [:title "ops-panel (WIP)"]]
                 [:body
                  [:h2 "Ops Panel (WIP)"]
                  [:div "An amazing ops panel will be here Soon&trade;!"]
                  [:div "This is your request:"]
                  [:pre (with-out-str (pp/pprint req))]
                  [:div "The stuff below brought to you by Clojurescript magic:"]
                  [:div#app_container
                   [:script {:type "text/javascript" :src "main.js"}]
                   [:script {:type "text/javascript"} "ops_panel.core.main();"]]])})

  ;; sente
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req))

  (files "/" {:root "target"})
  (resources "/" {:root "target"})
  (not-found "Page not found."))

(def app
  (wrap-defaults handler site-defaults))
