(ns buildkits.web
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.session.cookie :as cookie]
            [ring.middleware.resource :as resource]
            [ring.middleware.stacktrace :as trace]
            [ring.util.response :as res]
            [environ.core :as env]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.java.jdbc :as sql]
            [buildkits.db :as db]
            [buildkits.html :as html]
            [buildkits.kit :as kit]
            [buildkits.buildpack :as buildpack]))

(defn get-token [code]
  (-> (http/post "https://api.heroku.com/oauth/token"
                 {:form-params {:client_id (env/env :oauth-client-id)
                                :client_secret (env/env :oauth-client-secret)
                                :code code :grant_type "authorization_code"}})
      (:body) (json/decode true) :access_token))

(defn get-username [token]
  (-> (http/get "https://api.heroku.com/user"
                {:headers {"Authorization" (str "Bearer " token)}})
      (:body) (json/decode true) :email))

(defroutes app
  (GET "/" {{username :username} :session :as req}
       {:body (sql/with-connection db/db
                (html/dashboard (db/get-buildpacks) username
                                (if username
                                  (or (db/get-kit username)
                                      (db/create-kit username)))))})
  (GET "/buildkit/:name.tgz" [name]
       (sql/with-connection db/db
         (if-let [kit (db/get-kit name)]
           {:status 200
            :headers {"Content-Type" "application/octet-stream"}
            :body (kit/compose name kit)}
           {:status 404})))
  (GET "/oauth" [code]
       (if code
         (assoc (res/redirect "/")
           :session {:username (get-username (get-token code))})
         {:status 403}))
  (GET "/logout" []
       (assoc (res/redirect "/") :session nil))
  ;; TODO: use compojure contexts to enforce login
  (PUT "/buildkit/:org/:buildpack/:pos" [org buildpack pos :as
                                         {{:keys [username]} :session}]
       (if username
         (do (sql/with-connection db/db
               (db/add-to-kit username org buildpack (Integer. pos)))
             (res/redirect "/"))
         {:status 403}))
  (DELETE "/buildkit/:org/:buildpack/:pos" [org buildpack :as
                                            {{:keys [username]} :session}]
          (if username
            (do (sql/with-connection db/db
                  (db/remove-from-kit username org buildpack))
                (res/redirect "/"))
            {:stauts 403}))
  (ANY "/*" {:as req}
       (buildpack/app req))
  (route/not-found "Not found"))

(defn -main [& [port]]
  (let [port (Integer. (or port (env/env :port)))
        store (cookie/cookie-store {:key (env/env :session-secret)})]
    (jetty/run-jetty (-> #'app
                         (resource/wrap-resource "static")
                         ;; (trace/wrap-stacktrace)
                         (handler/site {:session {:store store}}))
                     {:port port :join? false})))

;; (def s (-main 8080))
