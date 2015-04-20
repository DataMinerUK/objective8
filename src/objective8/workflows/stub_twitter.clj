(ns objective8.workflows.stub-twitter
  (:require [bidi.ring :refer [make-handler]]
            [ring.util.response :as response]
            [objective8.workflows.sign-up :refer [sign-up-workflow]]
            [objective8.utils :as utils]))

;; Stub out twitter authentication workflow

(def twitter-id (atom "twitter-FAKE_ID"))

(defn update-stubbed-twitter-id [{:keys [params] :as request}]
  (if-let [new-id (:twitter-id params)]
    (do
      (reset! twitter-id (str "twitter-" new-id))
      {:headers {"Content-Type" "application/json"}
       :status 200
       :body {:twitter-id new-id}})
    {:status 500 
     :body "Resetting stubbed twitter id failed"}))

(defn stub-twitter-handler [request]
  (let [session (:session request)]
    (prn "Stubbing twitter with fake twitter id: " @twitter-id)
    (-> (response/redirect (str utils/host-url "/sign-up"))
        (assoc :session (assoc session 
                               :twitter-id @twitter-id
                               :twitter-screen-name "I'm a teapot")))))

(def stub-twitter-routes
  ["/" {"twitter-sign-in"        :stub-twitter
        "set-stubbed-twitter-id" :update-stub-twitter}])

(def stub-twitter-handlers
  {:stub-twitter         stub-twitter-handler
   :update-stub-twitter  update-stubbed-twitter-id})

(def stub-twitter-workflow
  (make-handler stub-twitter-routes stub-twitter-handlers))

