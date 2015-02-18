(ns objective8.api-test
  (:use org.httpkit.fake)
  (:require [org.httpkit.client :as http]
            [midje.sweet :refer :all]
            [cheshire.core :as json]
            [objective8.http-api :as api]
            [objective8.utils :as utils]))

(def host-url utils/host-url)

;USERS
(def USER_ID 1)
(def the-user {:twitter-id "twitter-TWITTER_ID"
               :email-address "blah@blah.com"})

(def the-stored-user (into the-user {:_id USER_ID}))

(def BEARER_NAME "bearer")
(def BEARER_TOKEN "token")

(def request-with-bearer-token (contains {:headers (contains {"api-bearer-token" BEARER_TOKEN
                                                               "api-bearer-name" BEARER_NAME})}))

(background (api/get-api-credentials) => {"api-bearer-name" BEARER_NAME
                                          "api-bearer-token" BEARER_TOKEN})

(facts "about user profiles"
       (fact "creating a user profile requires credentials"
             (api/create-user the-user) => the-stored-user
             (provided (api/post-request (contains "/api/v1/users")
                                         request-with-bearer-token)
                       => {:status 201
                           :headers {"Content-Type" "application/json"}
                           :body (json/generate-string the-stored-user)}))

       (fact "returns api-failure when post fails"
             (api/create-user the-user) => api/api-failure
             (provided (api/post-request anything anything)
                       => {:status 500})))

;OBJECTIVES
(def OBJECTIVE_ID 234)

(def the-objective {:title "My Objective"
                    :goals "To rock out, All day"
                    :description "I like cake"
                    :end-date "2015-01-31"
                    :username "my username"})

(def the-stored-objective (into the-objective {:_id OBJECTIVE_ID}))

(facts "about posting objectives"
       (fact "returns a stored objective when post succeeds"
             (api/create-objective the-objective) => the-stored-objective
             (provided (api/post-request (contains "/api/v1/objectives")
                                         request-with-bearer-token)
                       => {:status 201
                           :headers {"Content-Type" "application/json"}
                           :body (json/generate-string the-stored-objective)}))

       (fact "returns api-failure when post fails"
             (api/create-objective the-objective) => api/api-failure
             (provided (api/post-request anything anything) => {:status 500})))

(facts "about getting objectives"
       (fact "returns a stored objective when one exists with given id"
             (with-fake-http [(str host-url "/api/v1/objectives/" OBJECTIVE_ID)
                              {:status 200
                               :headers {"Content-Type" "application/json"}
                               :body (json/generate-string
                                       {:_id OBJECTIVE_ID
                                        :title "Objective title"
                                        :goals "Objective goals"
                                        :description "Objective description"
                                        :end-date "2015-01-31T00:00:00.000Z"})}]
               (api/get-objective OBJECTIVE_ID))
             => (contains {:_id OBJECTIVE_ID
                           :title "Objective title"
                           :goals "Objective goals"
                           :description "Objective description"
                           :end-date (utils/time-string->date-time "2015-01-31T00:00:00.000Z")}))
       (fact "returns 404 when no objective found"
             (with-fake-http [(str host-url "/api/v1/objectives/" OBJECTIVE_ID)
                              {:status 404}]
               (api/get-objective OBJECTIVE_ID))
             => (contains {:status 404})))

;; COMMENTS

(def the-comment {:comment "The comment"
                  :objective-id OBJECTIVE_ID
                  :username "my username"})

(def the-stored-comment (into the-comment {:_id 1}))

(facts "about posting comments"
       (fact "returns a stored comment when post succeeds"
             (api/create-comment the-comment) => the-stored-comment
             (provided (api/post-request (contains "/api/v1/comments")
                                         request-with-bearer-token)
                       =>{:status 201
                          :headers {"Content-Type" "application/json"}
                          :body (json/generate-string the-stored-comment)}))

       (fact "returns api-failure when post fails"
             (api/create-comment the-comment) => api/api-failure
             (provided (api/post-request anything anything) => {:status 500})))

(facts "about retrieving comments"
       (fact "returns a list of comments for an objective"
             (with-fake-http [(str host-url "/api/v1/objectives/1/comments") {:status 200
                                                                              :headers {"Content-Type" "application/json"}
                                                                              :body (json/generate-string [the-stored-comment])}]
               (api/retrieve-comments 1))
             => [the-stored-comment]))

;; QUESTIONS

(def the-question {:question "The meaning of life?"
                   :objective-id OBJECTIVE_ID
                   :created-by USER_ID})

(def QUESTION_ID 42)

(def the-stored-question (into the-question {:_id QUESTION_ID}))
(def question-response
  {:successful {:status 201
                :headers {"Content-Type" "application/json"}
                :body (json/generate-string the-stored-question)}
   :failure    {:status 500}})

(facts "about posting questions"
       (fact "returns a stored question when post succeeds"
             (with-fake-http [(str host-url "/api/v1/objectives/" OBJECTIVE_ID "/questions") (:successful question-response)]
               (api/create-question the-question))
             => the-stored-question)

       (fact "returns api-failure when post fails"
             (with-fake-http [(str host-url "/api/v1/objectives/" OBJECTIVE_ID "/questions") (:failure question-response)]
               (api/create-question the-question))
             => api/api-failure))

(facts "about getting questions"
       (fact "returns a stored question when one exists with a given id"
             (with-fake-http [(str host-url "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID) {:status 200
                                                                                                           :headers {"Content-Type" "application/json"}
                                                                                                           :body (json/generate-string the-stored-question)}]
               (api/get-question OBJECTIVE_ID QUESTION_ID))
             => {:status ::api/success
                 :result the-stored-question})

       (fact "returns :objective8.http-api/error when request fails"
             (with-fake-http [(str host-url "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID) {:status 500}]
               (api/get-question OBJECTIVE_ID QUESTION_ID))
             => {:status ::api/error})

       (fact "returns :objective8.http-api/not-found when no question with that id exists"
             (with-fake-http [(str host-url "/api/v1/objectives/" OBJECTIVE_ID "/questions/" QUESTION_ID) {:status 404}]
               (api/get-question OBJECTIVE_ID QUESTION_ID))
             => {:status ::api/not-found}))

;; ANSWERS

(def the-answer {:answer "The answer"
                 :objective-id OBJECTIVE_ID
                 :question-id QUESTION_ID})
(def the-stored-answer (into the-answer {:_id 3}))

(def answer-response
  {:successful {:status 201
                :headers {"Content-Type" "application/json"}
                :body (json/generate-string the-stored-answer)}
   :failure    {:status 500}})

(facts "about posting answers"
       (fact "returns a stored answer when post succeeds"
             (with-fake-http [(str host-url "/api/v1/objectives/" OBJECTIVE_ID
                                   "/questions/" QUESTION_ID "/answers") 
                              (:successful answer-response)]
               (api/create-answer the-answer))
             => {:status ::api/success
                 :result the-stored-answer}) 

       (fact "returns :objective8.http-api/error when request fails"
             (with-fake-http [(str host-url "/api/v1/objectives/" OBJECTIVE_ID
                                   "/questions/" QUESTION_ID "/answers") 
                              (:failure answer-response)]
               (api/create-answer the-answer))
             => {:status ::api/error}))
