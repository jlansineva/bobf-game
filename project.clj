(defproject clojure-bot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [cheshire "5.11.0"]
                 [clj-http "3.12.3"]
                 [conspiravision/pelinrakentaja-engine "0.1.0-SNAPSHOT"]]
  :main clojure-bot.core
  :plugins [[cider/cider-nrepl "0.30.0"]]
  :aot :all
  :jvm-opts ["-XstartOnFirstThread"]
  :target-path "target/%s"
  :profiles {:dev {:aot :all}
             :uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
