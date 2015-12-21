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
;; Data
;;------------------------------------------------------------------------------



;;------------------------------------------------------------------------------
;; App State
;;------------------------------------------------------------------------------

(def initial-page-state
  {:active-tab :classes
   :classes-search-text ""
   :sort-classes-by "class-names-a-z"})

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

(defn- change-class-search [js-evt]
  (let [new-text (aget js-evt "currentTarget" "value")]
    (swap! page-state assoc :classes-search-text new-text)))

(defn- on-change-classes-sort-by [js-evt]
  (let [new-value (aget js-evt "currentTarget" "value")]
    (swap! page-state assoc :sort-classes-by new-value)))

(defn- click-new-class-btn []
  (swap! page-state assoc :new-class-modal-showing? true))

(rum/defc UtilBody < rum/static
  []
  [:div "TODO: Util Body"])

(rum/defc FilesBody < rum/static
  []
  [:div "TODO: Files Body"])

(rum/defc ClassDetailBody < rum/static
  []
  [:div.right
    [:h2.class-name-sb0bc "primary-btn" [:span.muted "-sb974"]
      [:a {:href "#"} "rename"]]
    ;; TODO: make "files" be a dashed underline
    [:p "Used 32 times in 8 " [:span {:style {:text-decoration "underline"}} "files"]
      ". Found on " [:code "<button>"] " elements."]
    [:div.flex-container-s6d73
      [:div.definition-s7e19
        [:h4.header-s63ca "Definition"]
        [:div "TODO: definition here"]]
      [:div.preview-s376a
        [:h4.header-s63ca "Preview"]
        [:div "TODO: preview here"]]]])

(rum/defc ClassesSortByOptions < rum/static
  [sort-by]
  [:select
    {:on-change on-change-classes-sort-by
     :value sort-by}
    [:option {:value "class-names-a-z"} "Class Names A-Z"]
    [:option {:value "class-names-z-a"} "Class Names Z-A"]
    [:option {:value "files-a-z"} "Files A-Z"]
    [:option {:value "files-z-a"} "Files Z-A"]
    [:option {:value "most-used"} "Most Used"]
    [:option {:value "least-used"} "Least Used"]])

(rum/defc ClassesLeftPanel < rum/static
  [state]
  [:div.left
    [:input.search-input-s24fa
      {:on-change change-class-search
       :placeholder "Search Classes"
       :type "text"
       :value (:classes-search-text state)}]
    [:label.sort-by-s7ff8 "Sort By:"
      (ClassesSortByOptions (:sort-classes-by state))]
    [:ul.class-list
      [:li "active-8ff04"]
      [:li "clr-7f0e5"]
      [:li "tabs-sd691"]]])

(rum/defc ClassesBody < rum/static
  [state]
  [:div
    [:button.primary-s04e4
      {:on-click click-new-class-btn} "New Class"]
    [:div.flex-container-s6d73
      (ClassesLeftPanel state)
      (ClassDetailBody state)]])

(rum/defc Tabs < rum/static
  [active-tab]
  [:ul.tabs-sd691
    [:li [:a {:class (when (= active-tab :classes) "active-sb974")
              :href "#/classes"} "Classes"]]
    [:li [:a {:class (when (= active-tab :files) "active-sb974")
              :href "#/files"} "Files"]]
    [:li [:a {:class (when (= active-tab :util) "active-sb974")
              :href "#/util"} "Utilities"]]])

(rum/defc App < rum/static
  [state]
  [:div.container-53f43
    [:h1.title-5ac1e "Snowflake CSS"]
    (Tabs (:active-tab state))
    (condp = (:active-tab state)
      :classes
      (ClassesBody state)

      :files
      (FilesBody)

      :util
      (UtilBody)

      ;; NOTE: this should never happen
      :else
      [:div "Error: invalid :active-tab value"])])

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
