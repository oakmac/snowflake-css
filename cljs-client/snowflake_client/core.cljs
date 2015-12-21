(ns snowflake-client.core
  (:require
    [clojure.string :refer [blank?]]
    [rum.core :as rum]
    [snowflake-client.util :refer [by-id js-log log]]))

;;------------------------------------------------------------------------------
;; Data
;;------------------------------------------------------------------------------

(def dummy-class-data
  {"container-53f43"
    {:count 3
     :files
       [{:filename "src-cljs/my_proj/bar.cljs", :count 1}
        {:filename "src-cljs/my_proj/biz.cljs", :count 1}
        {:filename "src-cljs/my_proj/foo.cljs", :count 1}]}

   "label-4a5cc"
     {:count 22
      :files
        [{:filename "src-cljs/my_proj/bar.cljs", :count 5}
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
         {:filename "templates/volos.mustache", :count 1}]}

   "header-label-f23ea"
     {:count 6
      :files
        [{:filename "src-cljs/my_proj/bar.cljs", :count 2}
         {:filename "src-cljs/my_proj/biz.cljs", :count 2}
         {:filename "src-cljs/my_proj/foo.cljs", :count 2}]}

   "list-label-27ddb"
     {:count 8
      :files
        [{:filename "templates/larissa.mustache", :count 8}]}


   "clr-7f0e5"
     {:count 41
      :files []}})

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

(def initial-page-state
  {:classes (all-classes-sorted-by-name)
   :selected-classname nil
   :selected-file nil
   :files nil
   :search-text ""})

(def page-state (atom initial-page-state))

;;------------------------------------------------------------------------------
;; Events
;;------------------------------------------------------------------------------

(defn- on-change-input [js-evt]
  (let [new-text (aget js-evt "currentTarget" "value")]
    (swap! page-state assoc :search-text new-text)))

(defn- mark-active [active-idx idx itm]
  (if (= active-idx idx)
    (assoc itm :active? true)
    itm))

(defn- on-mouse-enter-class-row [js-evt]
  (let [target-el (aget js-evt "currentTarget")
        active-idx (int (.getAttribute target-el "data-idx"))
        classname (.getAttribute target-el "data-classname")
        current-class-list (:classes @page-state)
        class-list-with-no-active (map #(dissoc % :active?) current-class-list)
        new-class-list (map-indexed (partial mark-active active-idx) class-list-with-no-active)]
    ;; defensive - make sure the class exists
    (when (get @all-classes classname false)
      (swap! page-state assoc :classes (into [] new-class-list)
                              :files (get-in @all-classes [classname :files])
                              :selected-classname classname))))

;;------------------------------------------------------------------------------
;; Components
;;------------------------------------------------------------------------------

(rum/defc FileView < rum/static
  []
  [:div.fileview-col-c79be "*shows how the classname is used in the file*"])

(rum/defc FileRow < rum/static
  [f]
  [:div.file-row-c9a43 (:filename f)])

(rum/defc FilesList < rum/static
  [[selected-classname files]]
  [:div.files-col-74a77
    [:h4.col-title-2c774 "Files"
      [:div.count-13cac (str (count files) " files contain " selected-classname)]]
    ;; (str "Files that contain: " selected-classname)
    [:div.list-wrapper-7462e
      (map FileRow files)]])

(rum/defc ClassRow < rum/static
  [[idx c]]
  [:div {:class (str "single-class-f529a" (when (:active? c) " active-8ff04"))
         :data-classname (:classname c)
         :data-idx idx
         :on-mouse-enter on-mouse-enter-class-row}
    (:classname c)
    [:div.small-facts-6b6e3
      (str (count (:files c)) " files, "
           (:count c) " instances")]])

(rum/defc ClassList < rum/static
  [class-list]
  [:div.class-col-5acc6
    [:h4.col-title-2c774 "Classes"
      [:div.count-13cac (str (count class-list) " total")]]
    [:div.list-wrapper-7462e
      (map-indexed #(ClassRow [%1 %2]) class-list)]])

(rum/defc MainInput < rum/static
  [search-text]
  [:input.main-input-f14b8 {
                            :on-change on-change-input
                            :placeholder "Search classes and files"
                            :type "text"
                            :value search-text}])

(rum/defc App < rum/static
  [state]
  [:div.container-53f43
    [:h1.title-5ac1e "Snowflake CSS"]
    (MainInput (:search-text state))
    [:div.wrapper-463b0
      (ClassList (:classes state))
      (FilesList [(:selected-classname state) (:files state)])
      (FileView)
      [:div.clr-7f0e5]]])

;;------------------------------------------------------------------------------
;; Render Loop
;;------------------------------------------------------------------------------

(def app-container-el (by-id "appContainer"))

(defn- on-change-page-state [_kwd _the-atom _old-state new-state]
  ;; render the new state
  (rum/request-render
    (rum/mount (App new-state) app-container-el)))

(add-watch page-state :main on-change-page-state)

;;------------------------------------------------------------------------------
;; Global App Init
;;------------------------------------------------------------------------------

(defn- init!
  "Global app init."
  []
  (swap! page-state identity))

(.addEventListener js/window "load" init!)
