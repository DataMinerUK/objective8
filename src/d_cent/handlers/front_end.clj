(ns d-cent.handlers.front-end
  (:require [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [ring.util.response :as response]
            [cheshire.core :as json]
            [org.httpkit.client :as http]
            [d-cent.responses :refer :all]
            [d-cent.objectives :refer [request->objective find-by-id]]
            [d-cent.http-api :as api]
            [d-cent.utils :as utils]
            [d-cent.storage :as storage]))

;; HELPERS

(defn signed-in? []
  (friend/authorized? #{:signed-in} friend/*identity*))


;; HANDLERS

(defn index [{:keys [t' locale]}]
  (rendered-response index-page {:translation t'
                                 :locale (subs (str locale) 1)
                                 :doc-title (t' :index/doc-title)
                                 :doc-description (t' :index/doc-description)
                                 :signed-in (signed-in?)}))

(defn sign-in [{:keys [t' locale]}]
  (rendered-response sign-in-page {:translation t'
                                   :locale (subs (str locale) 1)
                                   :doc-title (t' :sign-in/doc-title)
                                   :doc-description (t' :sign-in/doc-description)
                                   :signed-in (signed-in?)}))

(defn sign-out [_]
  (friend/logout* (response/redirect "/")))

;; user profile

(defn create-user-profile-form [{:keys [t' locale]}]
  (rendered-response users-email {:translation t'
                                  :locale (subs (str locale) 1)
                                  :doc-title (t' :users-email/doc-title)
                                  :doc-description (t' :users-email/doc-description)
                                  :signed-in (signed-in?)}))

(defn create-user-profile-form-post [request]
  (let [user-id (:username (friend/current-authentication))
        email-address (get-in request [:params :email-address])]
    (if-let [stored-user-profile (api/create-user-profile {:user-id user-id :email-address email-address})]
      (response/redirect "/")
      {:status 404})))

;; objectives

(defn create-objective-form [{:keys [t' locale]}]
  (rendered-response objective-create-page {:translation t'
                                            :locale (subs (str locale) 1)
                                            :doc-title (t' :objective-create/doc-title)
                                            :doc-description (t' :objective-create/doc-description)
                                            :signed-in (signed-in?)}))

(defn create-objective-form-post [{:keys [t' locale] :as request}]
    (if-let [objective (request->objective request)]
      (if-let [stored-objective (api/create-objective objective)]
        (let [objective-url (str utils/host-url "/objectives/" (:_id stored-objective))]
          (assoc (response/redirect objective-url) :flash (t' :objective-view/created-message)))
        {:status 502})
      {:status 400}))

(defn objective [{:keys [t' locale] :as request}]
  (let [objective (find-by-id (storage/request->store request)
                                        (-> request :route-params :id))
        message (request :flash)]
    (rendered-response objective-view-page {:translation t'
                                            :locale (subs (str locale) 1)
                                            :doc-title (t' :objective-view/doc-title)
                                            :doc-description (t' :objective-view/doc-description)
                                            :message message
                                            :objective objective
                                            :signed-in (signed-in?)})))
