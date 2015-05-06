(ns snowflake-client.core
  (:require
    [clojure.string :refer [blank?]]
    [quiescent :include-macros true]
    [sablono.core :as sablono :include-macros true]
    [snowflake-client.dom :refer [by-id]]
    [snowflake-client.util :refer [js-log log]]))

;;------------------------------------------------------------------------------
;; Data
;;------------------------------------------------------------------------------

(def dummy-class-data {
  "container-53f43" {
    :count 3
    :files [
      {:filename "src-cljs/my_proj/bar.cljs", :count 1}
      {:filename "src-cljs/my_proj/biz.cljs", :count 1}
      {:filename "src-cljs/my_proj/foo.cljs", :count 1}
    ]
  }

  "label-4a5cc" {
    :count 22
    :files [
      {:filename "src-cljs/my_proj/bar.cljs", :count 5}
      {:filename "src-cljs/my_proj/biz.cljs", :count 2}
      {:filename "src-cljs/my_proj/foo.cljs", :count 3}
      {:filename "templates/athens.mustache", :count 1}
      {:filename "templates/chalcis.mustache", :count 1}
      {:filename "templates/chania.mustache", :count 1}
      {:filename "templates/heraklion.mustache", :count 1}
      {:filename "templates/ioannina.mustache", :count 1}
      {:filename "templates/larissa.mustache", :count 1}
      {:filename "templates/patras.mustache", :count 1}
      {:filename "templates/rhodes.mustache", :count 1}
      {:filename "templates/thessaloniki.mustache", :count 1}
      {:filename "templates/volos.mustache", :count 1}
    ]
  }

  "header-label-f23ea" {
    :count 6
    :files [
      {:filename "src-cljs/my_proj/bar.cljs", :count 2}
      {:filename "src-cljs/my_proj/biz.cljs", :count 2}
      {:filename "src-cljs/my_proj/foo.cljs", :count 2}
    ]
  }

  "list-label-27ddb" {
    :count 8
    :files [
      {:filename "templates/larissa.mustache", :count 8}
    ]
  }

  "clr-7f0e5" {
    :count 41
    :files [

    ]
  }
})

(def all-classes (atom dummy-class-data))

(defn- alpha-sort [a b]
  (compare (:classname a) (:classname b)))

(defn- add-classname [[classname c]]
  (assoc c :classname classname))

(defn- all-classes-sorted-by-name []
  (->> @all-classes
       (map add-classname)
       (sort alpha-sort)
       (into [])))

;;------------------------------------------------------------------------------
;; App State
;;------------------------------------------------------------------------------

(def initial-app-state {
  :classes (all-classes-sorted-by-name)
  :selected-classname nil
  :selected-file nil
  :files nil
  :search-text ""})

(def app-state (atom initial-app-state))

;;------------------------------------------------------------------------------
;; Events
;;------------------------------------------------------------------------------

(defn- on-change-input [js-evt]
  (let [new-text (aget js-evt "currentTarget" "value")]
    (swap! app-state assoc :search-text new-text)))

(defn- mark-active [active-idx idx itm]
  (if (= active-idx idx)
    (assoc itm :active? true)
    itm))

(defn- on-mouse-enter-class-row [js-evt]
  (let [target-el (aget js-evt "currentTarget")
        active-idx (int (.getAttribute target-el "data-idx"))
        classname (.getAttribute target-el "data-classname")
        current-class-list (:classes @app-state)
        class-list-with-no-active (map #(dissoc % :active?) current-class-list)
        new-class-list (map-indexed (partial mark-active active-idx) class-list-with-no-active)]
    ;; defensive - make sure the class exists
    (when (get @all-classes classname false)
      (swap! app-state assoc :classes (into [] new-class-list)
                             :files (get-in @all-classes [classname :files])
                             :selected-classname classname))))

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
    [:div.fileview-col-c79be "*shows how the classname is used in the file*"]))

(quiescent/defcomponent FileRow [f]
  (sablono/html
    [:div.file-row-c9a43
      (:filename f)]))

(quiescent/defcomponent FilesList [[selected-classname files]]
  (sablono/html
    [:div.files-col-74a77
      [:h4.col-title-2c774 "Files"
        [:div.count-13cac (str (count files) " files contain " selected-classname)]]
      ;; (str "Files that contain: " selected-classname)
      [:div.list-wrapper-7462e
        (map FileRow files)]]))

(quiescent/defcomponent ClassRow [[idx c]]
  (sablono/html
    [:div {:class (str "single-class-f529a" (when (:active? c) " active-8ff04"))
           :data-classname (:classname c)
           :data-idx idx
           :on-mouse-enter on-mouse-enter-class-row}
      (:classname c)
      [:div.small-facts-6b6e3
        (str (count (:files c)) " files, "
             (:count c) " instances")]]))

(quiescent/defcomponent ClassList [class-list]
  (sablono/html
    [:div.class-col-5acc6
      [:h4.col-title-2c774 "Classes"
        [:div.count-13cac (str (count class-list) " total")]]
      [:div.list-wrapper-7462e
        (map-indexed #(ClassRow [%1 %2]) class-list)]]))

(quiescent/defcomponent MainInput [search-text]
  (sablono/html
    [:input.main-input-f14b8 {
      :on-change on-change-input
      :placeholder "Search classes and files"
      :type "text"
      :value search-text}]))

(quiescent/defcomponent App [state]
  (sablono/html
    [:div.container-53f43
      [:h1.title-5ac1e "Snowflake CSS"]
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
