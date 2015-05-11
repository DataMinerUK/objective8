(ns objective8.front-end-helpers
  (:require [cemerick.friend :as friend]
            [objective8.utils :as utils]
            [objective8.sanitiser :as sanitiser]))

(defn request->invitation-info
  "Returns a map with the invitation details if all the parts are in the request. Otherwise return nil"
  [{:keys [params route-params] :as request} user-id]
  (when-let [objective-id (some-> (:id route-params)
                                  Integer/parseInt)]
    (some-> params
            (utils/select-all-or-nothing [:writer-name :writer-email :reason])
            (assoc :objective-id objective-id :invited-by-id user-id))))

(defn request->profile-info [{:keys [params] :as request} user-id]
  (some-> params
          (utils/select-all-or-nothing [:name :biog])
          (assoc :user-uri (str "/users/" user-id))))

(defn request->up-vote-info [{:keys [params] :as request} user-id]
  (some-> params
          (utils/select-all-or-nothing [:vote-on-uri])
          (assoc :created-by-id user-id :vote-type "up")))

(defn request->down-vote-info [{:keys [params] :as request} user-id]
  (some-> params
          (utils/select-all-or-nothing [:vote-on-uri])
          (assoc :created-by-id user-id :vote-type "down"))) 

(defn request->star-info [{:keys [params] :as request} user-id]
  (when-let [objective-uri (:objective-uri params)]
    {:objective-uri objective-uri :created-by-id user-id}))

(defn request->draft-info [{:keys [params] :as request} user-id]
  (some-> params
          (utils/select-all-or-nothing [:id :google-doc-html-content])
          (assoc :submitter-id user-id)
          (utils/ressoc :id :objective-id)
          (update-in [:objective-id] #(Integer/parseInt %))
          (utils/ressoc :google-doc-html-content :content)
          (update-in [:content] #(utils/html->hiccup (sanitiser/sanitise-html %)))))

(defn request->mark-info [{:keys [params] :as request} user-id]
  (some-> params
          (utils/select-all-or-nothing [:question-uri])
          (assoc :created-by-uri (str "/users/" user-id))))

(defn request->admin-removal-confirmation-info [{:keys [params] :as request} user-id]
  (some-> params
          (utils/select-all-or-nothing [:removal-uri])
          (assoc :removed-by-uri (str "/users/" user-id))))

(defn request->admin-removal-info [{:keys [params] :as request}]
  (utils/select-all-or-nothing params [:removal-uri :removal-sample]))

(defn flash->removal-data [{:keys [type data] :as flash}]
  (when (= type :flash-data)
    (utils/select-all-or-nothing data [:removal-uri :removal-sample])))
