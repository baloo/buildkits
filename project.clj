(defproject buildkits "0.1.0-SNAPSHOT"
  :description "For managing which sets of buildpacks to use."
  :url "http://buildkits.herokuapp.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [compojure "1.1.0"]
                 [com.cemerick/friend "0.0.8"]
                 [cheshire "4.0.0"]
                 [hiccup "1.0.0"]
                 [postgresql "9.1-901-1.jdbc4"]
                 [org.clojure/java.jdbc "0.2.1"]
                 [com.heroku.api/heroku-api "0.9"]])