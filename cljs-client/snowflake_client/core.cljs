(ns snowflake-client.core
  (:require
    [quiescent :include-macros true]
    [sablono.core :as sablono :include-macros true]
    [snowflake-client.dom :refer [by-id]]
    [snowflake-client.util :refer [js-log log]]))

;;------------------------------------------------------------------------------
;; Data
;;------------------------------------------------------------------------------

(def dummy-class-list {
  "container-53f43" {
    :count 3
  }

  "label-4a5cc" {
    :count 22
  }

  "header-label-f23ea" {
    :count 6
  }

  "list-label-27ddb" {
    :count 1
  }

  "clr-7f0e5" {
    :count 41
  }
})

(def all-classes (atom dummy-class-list))

(defn- alpha-sort [[a-classname _a-map] [b-classname _b-map]]
  (compare a-classname b-classname))

(defn- all-classes-sorted-by-name []
  (into [] (sort alpha-sort @all-classes)))

;;------------------------------------------------------------------------------
;; App State
;;------------------------------------------------------------------------------

(def initial-app-state {
  :classes (all-classes-sorted-by-name)
  :selected-classname nil
  :selected-file nil
  :files nil
  :search-text ""
  })

(def app-state (atom initial-app-state))

;;------------------------------------------------------------------------------
;; Events
;;------------------------------------------------------------------------------

(defn- on-change-input [js-evt]
  (let [new-text (aget js-evt "currentTarget" "value")]
    (swap! app-state assoc :search-text new-text)))

(defn- on-mouse-enter-class [js-evt]
  (let [target-el (aget js-evt "currentTarget")
        idx (int (.getAttribute target-el "data-idx"))
        classname (.getAttribute target-el "data-classname")]
    (swap! app-state assoc :selected-classname classname)))

;;------------------------------------------------------------------------------
;; Templates
;;------------------------------------------------------------------------------

(sablono/defhtml empty-files-list []
  [:div "No files found."]
  )

;;------------------------------------------------------------------------------
;; Components
;;------------------------------------------------------------------------------

(quiescent/defcomponent FileView []
  (sablono/html
    [:div.half-col-c79be "FileView"]))

(quiescent/defcomponent FileRow [f]
  (sablono/html
    [:div.file-row-c9a43
      (:filename f)]))

(quiescent/defcomponent FilesList [[selected-classname files]]
  (sablono/html
    [:div.quarter-col-5acc6
      (str "Files that contain: " selected-classname)
      ;;(map FileRow files)
      ]))

(quiescent/defcomponent AClass [[idx [classname c]]]
  (sablono/html
    [:div.single-class-f529a
      {:data-classname classname
       :data-idx idx
       :on-mouse-enter on-mouse-enter-class}
      classname]))

(quiescent/defcomponent ClassList [class-list]
  (sablono/html
    [:div.quarter-col-5acc6
      (map-indexed #(AClass [%1 %2]) class-list)]))

(quiescent/defcomponent MainInput [search-text]
  (sablono/html
    [:input.main-input-f14b8 {
      :on-change on-change-input
      :placeholder "Search classes"
      :type "text"
      :value search-text}]))

(quiescent/defcomponent App [state]
  (sablono/html
    [:div.container-53f43
      [:h1 "Snowflake CSS"]
      (MainInput (:search-text state))
      [:div.wrapper-463b0
        (ClassList (:classes state))
        (FilesList [(:selected-classname state) (:files state)])
        (FileView)
        [:div.clr-7f0e5]]]))

;;------------------------------------------------------------------------------
;; Render Loop
;;------------------------------------------------------------------------------

(def app-container-el (by-id "appContainer"))

;; TODO: queue this on requestAnimationFrame
(defn- on-change-app-state [_keyword _the-atom _old-state new-state]

  ; (log new-state)
  ; (js-log "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")

  (quiescent/render (App new-state) app-container-el))

(add-watch app-state :main on-change-app-state)

;;------------------------------------------------------------------------------
;; Global App Init
;;------------------------------------------------------------------------------

(defn- init!
  "Global app init."
  []
  (swap! app-state identity))

(.addEventListener js/window "load" init!)
